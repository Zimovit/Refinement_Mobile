package org.zimovit.refinement_mobile;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.math.BigInteger;

public class MainActivity extends AppCompatActivity {

    //ported from desktop app
    private static final int MATERIAL_COST = 25000;
    private static final int REFINEMENT_COST = 10000;
    private static int itemPrice;
    private static double chanceToRefine = 0.5, chanceToBrake = 0.25;
    private static int[][] safeRefinementTable ={{1, 5, 100000}, {2, 10, 220000}, {3, 15, 470000}, {4, 25, 910000}, {6, 50, 1630000}, {10, 85, 2740000}};
    private EditText price, numberOfTriesField;

    //debugging tag
    private final String TAG = this.getClass().getSimpleName();

    Button calculateButton, cancelButton;

    //table array

    String[] cells = new String[24];

    GridView gv;
    ArrayAdapter<String> adapter;
    ProgressBar hProgressBar;
    private BackgroundProcess backgroundProcess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate initialized");
        setContentView(R.layout.activity_main);

        initializeViews();
        Log.d(TAG, "Views initialized");

    }

    private void initializeViews(){

        //creating table

        createBlankTable();

        //Table is formed

        adapter = new ArrayAdapter<>(this, R.layout.item, R.id.tvText, cells);
        gv = (GridView) findViewById(R.id.grid);
        gv.setAdapter(adapter);
        gv.setNumColumns(3);
        gv.setVerticalSpacing(5);
        gv.setHorizontalSpacing(5);

        //price and number of tries fields
        price = (EditText) findViewById(R.id.priceOfItem);
        numberOfTriesField = (EditText) findViewById(R.id.numberOfTriesField);

        //time to initialize button
        calculateButton = (Button) findViewById(R.id.counterButton);
        View.OnClickListener buttonClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate();
            }
        };
        calculateButton.setOnClickListener(buttonClicked);

        cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setEnabled(false);
        View.OnClickListener cancelClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backgroundProcess.cancel(true);
            }
        };
        cancelButton.setOnClickListener(cancelClicked);

        //and progress bar
        hProgressBar = findViewById(R.id.progressBar);
        hProgressBar.setMax(14);

    }

    //no longer main calculating method
    private void calculate() {
        int tries;

        try{
            itemPrice = Integer.parseInt(price.getText().toString().trim());
            tries = Integer.parseInt(numberOfTriesField.getText().toString().trim());
        }
        catch (NumberFormatException e){
            showError();
            return;
        }

        if (itemPrice <= 0 || tries <= 0) {
            showError();
            return;
        }

        //running background task
        backgroundProcess = new BackgroundProcess();
        backgroundProcess.execute(itemPrice, tries);

    }

    private void showError(){
        Toast toast = Toast.makeText(this, "Проверьте цену и количество попыток", Toast.LENGTH_SHORT);
        toast.show();
    }

    private static int unsafeRefinement(int startRefinement, int finalRefinement) throws IllegalArgumentException {
        //нужно проверить, есть ли смысл точиться
        if (startRefinement == finalRefinement) return 0;   //уже заточено
        if ((startRefinement > finalRefinement)) throw new IllegalArgumentException("Начальный уровень выше целевого");

        //и вообще верные ли параметры

        if (startRefinement < 4 || finalRefinement > 10) throw new IllegalArgumentException("Неверно указаны уровни заточки");

        int cost = 0;   //эта переменная будет хранить стоимость заточки от начальной до желаемой.

        boolean isBroken = false;  //отдаём в заточку целую вещь

        //refLevel - на этот уровень пытаемся точиться
        for (int refLevel = startRefinement+1; refLevel <= finalRefinement;){
            //платим за материалы и за заточку
            cost = cost + MATERIAL_COST + (REFINEMENT_COST * refLevel);

            if (isBroken) {
                cost = cost + itemPrice;  //чинимся
                isBroken = false;   //и починились
            }

            boolean success = Math.random() < chanceToRefine;  //пан или пропал
            if (success){
                //точнулись, оплата уже прошла, просто повышаем уровень и на норвый цикл
                refLevel++;
            } else {
                //а тут всё фигово. Для начала слетает заточка на 1
                if (refLevel > 4) refLevel--;

                //а ведь можно ещё и сломаться
                if (Math.random() < chanceToBrake) isBroken = true;  //хрусть

            }
        }

        return cost;

    }

    private static int safeRefinement(int goalLevel) throws IllegalArgumentException {
        //проверим корректность аргумента
        if (goalLevel < 5 || goalLevel > 10) throw new IllegalArgumentException("Неверно указан уровень заточки");

        int cost = 0;   //здесь собираем стоимость
        int iterNumber = goalLevel-5; //побежим по табличке, заточка на +5 - нулевая строка таблицы
        for (int i = 0; i <= iterNumber; i++){
            cost = cost + itemPrice*safeRefinementTable[i][0] + MATERIAL_COST*safeRefinementTable[i][1] + safeRefinementTable[i][2];
        }
        return cost;
    }

    private static int improvedRefinement(){
        int cost = 0;

        for (int i = 0; i < 5;){
            cost += 225000;
            if (Math.random() < chanceToRefine){
                i++;
            } else {
                i--;
                if (i < 0) {
                    cost = cost + unsafeRefinement(9, 10);
                    i = 0;
                }
                cost += itemPrice;
            }
        }
        return cost;
    }

    private void createBlankTable(){
        //first row
        cells[0] = "Уровень безопасной";
        cells[1] = "Средняя стоимость";
        cells[2] = "До +15";

        //consecutive rows
        for (int i = 3; i < 19; i += 3){
            cells[i] = "+"+((i-3)/3+5);
            cells[i+1] = "0";
            cells[i+2] = "0";
        }

        //last row
        cells[21] = "Небезопасная";
        cells[22] = "0";
        cells[23] = "0";
    }


    /**
     * AsyncTask private class to process in background
     * takes price and number of tries as parameters
     * and updates static cells array
     */
    private class BackgroundProcess extends AsyncTask<Integer, Integer, Void>{

        @Override
        protected void onPreExecute(){
            //just disabling the button to prevent task rerun
            calculateButton.setEnabled(false);
            cancelButton.setEnabled(true);
            //and zeroing progress bar
            hProgressBar.setProgress(0);
        }

        @Override
        protected void onCancelled(){
            calculateButton.setEnabled(true);
            cancelButton.setEnabled(false);
            hProgressBar.setProgress(0);
            createBlankTable();
            adapter.notifyDataSetChanged();
        }

        @Override
        protected Void doInBackground(Integer... integers) {


            int counter = 0;    //for progress bar

            //taking params
            int itemPrice = integers[0];
            int tries = integers[1];

            //calculating
            //точка до +4
            int refinementPrice = itemPrice + MATERIAL_COST*4 + REFINEMENT_COST*10;

            for (int i = 1; i < 7; i++){
                BigInteger unsafeMediumCost = new BigInteger("0");
                //a lot of tries
                for (int j = 0; j < tries; j++){
                    unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(unsafeRefinement(i+4, 10))));
                    if (isCancelled()) return null;
                }
                unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries))); //calculating medium cost
                int cost = refinementPrice + safeRefinement(i+4) + unsafeMediumCost.intValue();//final cost
                cells[i*3+1] = String.valueOf(cost);
                publishProgress(++counter);

                //calculating +15
                unsafeMediumCost = new BigInteger("0");
                for (int j = 0; j < tries; j++){
                    unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(improvedRefinement())));
                    if (isCancelled()) return null;
                }
                unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries)));
                cost = cost+unsafeMediumCost.intValue();
                cells[i*3+2] = String.valueOf(cost);
                publishProgress(++counter);
            }

            //totally unsafe
            BigInteger unsafeMediumCost = new BigInteger("0");
            for (int j = 0; j < tries; j++){
                unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(unsafeRefinement(4, 10))));
                if (isCancelled()) return null;
            }
            unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries)));
            int cost = refinementPrice + unsafeMediumCost.intValue();
            cells[22] = String.valueOf(cost);
            publishProgress(++counter);

            unsafeMediumCost = new BigInteger("0");
            for (int j = 0; j < tries; j++){
                unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(improvedRefinement())));
                if (isCancelled()) return null;
            }
            unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries)));

            cells[23] = String.valueOf(cost+unsafeMediumCost.intValue());
            publishProgress(++counter);

            return null;
        }

        @Override
        protected void onPostExecute(Void v){
            calculateButton.setEnabled(true);
            adapter.notifyDataSetChanged();
            cancelButton.setEnabled(false);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            hProgressBar.setProgress(values[0]);
        }

    }
}
