package org.zimovit.refinement_mobile;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigInteger;

public class MainActivity extends AppCompatActivity {

    //ported from desktop app
    private static final int MATERIAL_COST = 25000;
    private static final int IMPROVED_MATERIAL_COST = 125000;
    private static final int REFINEMENT_COST = 10000;
    private static int itemPrice;
    private static double chanceToRefine = 0.5, chanceToBrake = 0.25;
    private static int numberOfTries = 1000000;
    private static int[][] safeRefinementTable ={{1, 5, 100000}, {2, 10, 220000}, {3, 15, 470000}, {4, 25, 910000}, {6, 50, 1630000}, {10, 85, 2740000}};
    private EditText price, numberOfTriesField;

    //debugging tag
    private final String TAG = this.getClass().getSimpleName();

    Button calculateButton;

    //table:
    //rows
    TableRow[] rows = new TableRow[8];
    //cells
    TextView[][] cells = new TextView[7][3];

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

        //first row
        rows[0] = new TableRow(this);
        TextView plusTenLabel = new TextView(this);
        plusTenLabel.setText("С безопасной заточкой до уровня");
        //covers two cells
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.span = 2;
        //add to row
        rows[0].addView(plusTenLabel, params);
        //+15 tab
        TextView plus15Label = new TextView(this);
        plus15Label.setText("+15");
        rows[0].addView(plus15Label);

        //consecutive rows
        for (int i = 1; i < 8; i++){
            rows[i] = new TableRow(this);
            //filling row with cells
            cells[i-1][0] = new TextView(this);
            String string = "+" + (i+5);
            cells[i-1][0].setText(string);

            cells[i-1][1] = new TextView(this);
            cells[i-1][1].setText("0");
            cells[i-1][2] = new TextView(this);
            cells[i-1][2].setText("0");
            for (int j = 0; j < 3; j++) rows[i].addView(cells[i-1][j]);
        }

        TableLayout table = (TableLayout)findViewById(R.id.table);
        for (int i = 0; i < rows.length; i++) table.addView(rows[i]);

        //Table is formed

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

    }

    //main calculating method
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
        //точка до +4
        int refinementPrice = itemPrice + MATERIAL_COST*4 + REFINEMENT_COST*10;

        for (int i = 0; i < 6; i++){
            BigInteger unsafeMediumCost = new BigInteger("0");
            for (int j = 0; j < tries; j++){
                unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(unsafeRefinement(i+5, 10))));

            }
            unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries)));
            int cost = refinementPrice + safeRefinement(i+5) + unsafeMediumCost.intValue();
            cells[i][1].setText(String.valueOf(cost));
            unsafeMediumCost = new BigInteger("0");
            for (int j = 0; j < tries; j++){
                unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(improvedRefinement())));
            }
            unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries)));
            cost = cost+unsafeMediumCost.intValue();
            cells[i][2].setText(String.valueOf(cost));
        }

        BigInteger unsafeMediumCost = new BigInteger("0");
        for (int j = 0; j < tries; j++){
            unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(unsafeRefinement(4, 10))));

        }
        unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries)));
        int cost = refinementPrice + unsafeMediumCost.intValue();
        cells[6][1].setText(String.valueOf(cost));

        unsafeMediumCost = new BigInteger("0");
        for (int j = 0; j < tries; j++){
            unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(improvedRefinement())));
        }
        unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries)));

        cells[6][2].setText(String.valueOf(cost+unsafeMediumCost.intValue()));
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
}
