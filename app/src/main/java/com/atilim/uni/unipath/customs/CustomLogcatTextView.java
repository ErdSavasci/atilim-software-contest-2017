package com.atilim.uni.unipath.customs;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

import com.atilim.uni.unipath.SettingsActivity;

import es.munix.logcat.LogcatListener;
import es.munix.logcat.R.color;
import es.munix.logcat.R.styleable;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CustomLogcatTextView extends ScrollView implements LogcatListener {
    private int verboseColor;
    private int debugColor;
    private int errorColor;
    private int infoColor;
    private int warningColor;
    private int consoleColor;
    private TextView textView;

    public CustomLogcatTextView(Context context) {
        super(context);
        this.init((AttributeSet)null);
    }

    public CustomLogcatTextView(Context context, AttributeSet attrs) {
        super(context);
        this.init(attrs);
    }

    public CustomLogcatTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(attrs);
    }

    @TargetApi(21)
    public CustomLogcatTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init(attrs);
    }

    private void init(AttributeSet attrs) {
        this.textView = new TextView(this.getContext());
        this.textView.setLayoutParams(new LayoutParams(-1, -1));
        this.textView.setPadding(20, 20, 20, 20);
        this.addView(this.textView);
        this.textView.setTextColor(this.getContext().getResources().getColor(color.defaultTextColor));
        if(attrs != null) {
            TypedArray a = this.getContext().getTheme().obtainStyledAttributes(attrs, styleable.LogcatTextView, 0, 0);

            try {
                this.verboseColor = a.getColor(styleable.LogcatTextView_verboseColor, this.getContext().getResources().getColor(color.defaultVerboseColor));
                this.debugColor = a.getColor(styleable.LogcatTextView_debugColor, this.getContext().getResources().getColor(color.defaultDebugColor));
                this.errorColor = a.getColor(styleable.LogcatTextView_errorColor, this.getContext().getResources().getColor(color.defaultErrorColor));
                this.infoColor = a.getColor(styleable.LogcatTextView_infoColor, this.getContext().getResources().getColor(color.defaultInfoColor));
                this.warningColor = a.getColor(styleable.LogcatTextView_warningColor, this.getContext().getResources().getColor(color.defaultWarningColor));
                this.consoleColor = a.getColor(styleable.LogcatTextView_consoleColor, this.getContext().getResources().getColor(color.defaultConsoleColor));
            } finally {
                a.recycle();
            }
        } else {
            this.verboseColor = this.getContext().getResources().getColor(color.defaultVerboseColor);
            this.debugColor = this.getContext().getResources().getColor(color.defaultDebugColor);
            this.errorColor = this.getContext().getResources().getColor(color.defaultErrorColor);
            this.infoColor = this.getContext().getResources().getColor(color.defaultInfoColor);
            this.warningColor = this.getContext().getResources().getColor(color.defaultWarningColor);
            this.consoleColor = this.getContext().getResources().getColor(color.defaultConsoleColor);
        }

        this.setBackgroundColor(this.consoleColor);
        this.textView.setBackgroundColor(this.consoleColor);
    }

    public void refreshLogcat() {
        this.getLogcat(this);
    }

    public TextView getTextView(){
        return textView;
    }

    public void onLogcatCaptured(String logcat) {
        this.textView.setText(Html.fromHtml(logcat));
    }

    public void getLogcat(final LogcatListener listener) {
        (new Thread() {
            public void run() {
                try {
                    String e = Integer.toString(Process.myPid());
                    String[] command = new String[]{"logcat", "-d", "-v", "threadtime"};
                    java.lang.Process process = Runtime.getRuntime().exec(command);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    StringBuilder log = new StringBuilder();

                    String line;
                    while((line = bufferedReader.readLine()) != null) {
                        if(line.contains(e)) {
                            int lineColor = CustomLogcatTextView.this.verboseColor;
                            if(line.contains(" I ")) {
                                lineColor = CustomLogcatTextView.this.infoColor;
                            } else if(line.contains(" E ")) {
                                lineColor = CustomLogcatTextView.this.errorColor;
                            } else if(line.contains(" D ")) {
                                lineColor = CustomLogcatTextView.this.debugColor;
                            } else if(line.contains(" W ")) {
                                lineColor = CustomLogcatTextView.this.warningColor;
                            }

                            log.append("<font color=\"#").append(Integer.toHexString(lineColor).toUpperCase().substring(2)).append("\">").append(line).append("</font><br><br>");
                        }
                    }

                    listener.onLogcatCaptured(log.toString());
                } catch (Exception var8) {
                    var8.printStackTrace();
                }

            }
        }).start();
    }

    protected CustomLogcatTextView(Parcel in) {
        super(null);
        verboseColor = in.readInt();
        debugColor = in.readInt();
        errorColor = in.readInt();
        infoColor = in.readInt();
        warningColor = in.readInt();
        consoleColor = in.readInt();
        textView = (TextView) in.readValue(TextView.class.getClassLoader());
    }
}