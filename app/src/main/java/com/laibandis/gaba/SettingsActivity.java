package com.laibandis.gaba;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;

public class SettingsActivity extends Activity {

    public static final String PREFS = "kiss_prefs";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);

        // 💰 минимальная цена
        EditText price = new EditText(this);
        price.setHint("Минимальная цена (тг)");
        price.setInputType(InputType.TYPE_CLASS_NUMBER);
        price.setText(String.valueOf(p.getInt("min_price", 5000)));
        root.addView(price);

        // 🚕 только межгород
        Switch onlyIntercity = new Switch(this);
        onlyIntercity.setText("Только межгород");
        onlyIntercity.setChecked(p.getBoolean("only_intercity", true));
        root.addView(onlyIntercity);

        // 🚫 игнор городских
        Switch ignoreCity = new Switch(this);
        ignoreCity.setText("Игнорировать городские");
        ignoreCity.setChecked(p.getBoolean("ignore_city", true));
        root.addView(ignoreCity);

        // --- сохранение ---
        price.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int vPrice = Integer.parseInt(price.getText().toString());
                    if (vPrice < 1000) vPrice = 1000;
                    p.edit().putInt("min_price", vPrice).apply();
                } catch (Exception ignored) {}
            }
        });

        onlyIntercity.setOnCheckedChangeListener((b1, v) ->
                p.edit().putBoolean("only_intercity", v).apply()
        );

        ignoreCity.setOnCheckedChangeListener((b2, v) ->
                p.edit().putBoolean("ignore_city", v).apply()
        );

        setContentView(root);
    }
}
