package com.yournix.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/** Один экран, две вкладки: шифрование и расшифровка. Результат считается сразу, пока вы вводите текст. */
public class MainActivity extends Activity {

    private static final int ENCRYPT = 0;
    private static final int DECRYPT = 1;
    private static final String STATE_MODE = "mode";
    private static final String STATE_INPUTS = "inputs";

    private final HillCipher cipher = new HillCipher();
    private final String[] inputs = {"", ""}; // свой текст у каждой вкладки
    private int mode = ENCRYPT;
    private String result = "";

    private TextView tabEncrypt;
    private TextView tabDecrypt;
    private TextView resultView;
    private EditText input;
    private Button copyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabEncrypt = findViewById(R.id.tab_encrypt);
        tabDecrypt = findViewById(R.id.tab_decrypt);
        resultView = findViewById(R.id.result);
        input = findViewById(R.id.input);
        copyButton = findViewById(R.id.btn_copy);
        Button pasteButton = findViewById(R.id.btn_paste);
        Button clearButton = findViewById(R.id.btn_clear);

        if (savedInstanceState != null) {
            String[] saved = savedInstanceState.getStringArray(STATE_INPUTS);
            if (saved != null && saved.length == 2) {
                inputs[ENCRYPT] = saved[ENCRYPT];
                inputs[DECRYPT] = saved[DECRYPT];
            }
            mode = savedInstanceState.getInt(STATE_MODE, ENCRYPT);
        }

        tabEncrypt.setOnClickListener(v -> setMode(ENCRYPT));
        tabDecrypt.setOnClickListener(v -> setMode(DECRYPT));
        pasteButton.setOnClickListener(v -> paste());
        clearButton.setOnClickListener(v -> input.setText(""));
        copyButton.setOnClickListener(v -> copy());

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                inputs[mode] = s.toString();
                refreshResult();
            }
        });

        showMode();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_MODE, mode);
        outState.putStringArray(STATE_INPUTS, inputs);
    }

    private void setMode(int newMode) {
        if (newMode == mode) {
            return;
        }
        mode = newMode;
        showMode();
    }

    private void showMode() {
        boolean encrypt = mode == ENCRYPT;
        styleTab(tabEncrypt, encrypt);
        styleTab(tabDecrypt, !encrypt);
        input.setHint(encrypt ? R.string.hint_encrypt : R.string.hint_decrypt);
        input.setText(inputs[mode]);
        input.setSelection(input.length());
        refreshResult();
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.tab_selected : 0);
        tab.setTextColor(getColor(selected ? R.color.text : R.color.text_muted));
    }

    private void refreshResult() {
        String text = inputs[mode];
        boolean ok = false;
        result = "";
        if (text.trim().isEmpty()) {
            resultView.setText(mode == ENCRYPT ? R.string.placeholder_encrypt : R.string.placeholder_decrypt);
            resultView.setTextColor(getColor(R.color.panel_muted));
        } else {
            try {
                result = mode == ENCRYPT ? cipher.encrypt(text) : cipher.decrypt(text);
                resultView.setText(result);
                resultView.setTextColor(getColor(R.color.panel_text));
                ok = !result.isEmpty();
            } catch (IllegalArgumentException e) {
                result = "";
                resultView.setText(e.getMessage());
                resultView.setTextColor(getColor(R.color.panel_error));
            }
        }
        copyButton.setEnabled(ok);
        copyButton.setAlpha(ok ? 1f : 0.35f);
    }

    private void paste() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        CharSequence text = null;
        if (clip != null && clip.getItemCount() > 0) {
            text = clip.getItemAt(0).coerceToText(this);
        }
        if (text == null || text.length() == 0) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        input.setText(text.toString());
        input.setSelection(input.length());
    }

    private void copy() {
        if (result.isEmpty()) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Yournix", result));
        copyButton.setText(R.string.copied);
        copyButton.postDelayed(() -> copyButton.setText(R.string.copy), 1500);
    }
}
