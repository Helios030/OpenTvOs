package con.open.tvos.ui.dialog;

import android.content.Context;

import androidx.annotation.NonNull;

import con.open.tvos.R;

import org.jetbrains.annotations.NotNull;

public class AboutDialog extends BaseDialog {

    public AboutDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_about);
    }
}