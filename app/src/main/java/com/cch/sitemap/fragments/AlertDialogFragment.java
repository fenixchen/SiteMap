package com.cch.sitemap.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import static java.lang.annotation.ElementType.PACKAGE;

/**
 * {@link DialogFragment} that displays an {@link AlertDialog}.<br>
 */
public class AlertDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    // Tag
    public static final String TAG = AlertDialogFragment.class.getSimpleName();

    // Arguments
    private static final String KEY_TITLE_RES_ID = PACKAGE + ".key.TITLE_RES_ID";
    private static final String KEY_TITLE = PACKAGE + ".key.TITLE";
    private static final String KEY_MESSAGE_RES_ID = PACKAGE + ".key.MESSAGE_RES_ID";
    private static final String KEY_MESSAGE = PACKAGE + ".key.MESSAGE";
    private static final String KEY_POSITIVE_BUTTON_RES_ID = PACKAGE + ".key.POSITIVE_BUTTON_RES_ID";
    private static final String KEY_POSITIVE_BUTTON = PACKAGE + ".key.POSITIVE_BUTTON";
    private static final String KEY_NEGATIVE_BUTTON_RES_ID = PACKAGE + ".key.NEGATIVE_BUTTON_RES_ID";
    private static final String KEY_NEGATIVE_BUTTON = PACKAGE + ".key.NEGATIVE_BUTTON";

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment.
     */
    public AlertDialogFragment() {
    }

    /**
     * Factory method to create a new instance of {@link AlertDialogFragment}.
     *
     * @param titleResId          the title string resource identifier.
     * @param messageResId        the message string resource identifier.
     * @param positiveButtonResId the positive button string resource identifier.
     * @param negativeButtonResId the negative button string resource identifier.
     * @return a new instance of {@link AlertDialogFragment}.
     */
    public static AlertDialogFragment newInstance(int titleResId, int messageResId, int positiveButtonResId, int negativeButtonResId) {
        final AlertDialogFragment alertDialogFragment = new AlertDialogFragment();
        final Bundle arguments = new Bundle();
        arguments.putInt(KEY_TITLE_RES_ID, titleResId);
        arguments.putInt(KEY_MESSAGE_RES_ID, messageResId);
        if (positiveButtonResId != 0) {
            arguments.putInt(KEY_POSITIVE_BUTTON_RES_ID, positiveButtonResId);
        }
        if (negativeButtonResId != 0) {
            arguments.putInt(KEY_NEGATIVE_BUTTON_RES_ID, negativeButtonResId);
        }
        alertDialogFragment.setArguments(arguments);
        return alertDialogFragment;
    }

    /**
     * Factory method to create a new instance of {@link AlertDialogFragment}.
     *
     * @param title               the title text.
     * @param message             the message text.
     * @param positiveButtonResId the positive button string resource identifier.
     * @param negativeButtonResId the negative button string resource identifier.
     * @return a new instance of {@link AlertDialogFragment}.
     */
    public static AlertDialogFragment newInstance(String title, String message, int positiveButtonResId, int negativeButtonResId) {
        final AlertDialogFragment alertDialogFragment = new AlertDialogFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(KEY_TITLE, title);
        arguments.putString(KEY_MESSAGE, message);
        if (positiveButtonResId != 0) {
            arguments.putInt(KEY_POSITIVE_BUTTON_RES_ID, positiveButtonResId);
        }
        if (negativeButtonResId != 0) {
            arguments.putInt(KEY_NEGATIVE_BUTTON_RES_ID, negativeButtonResId);
        }
        alertDialogFragment.setArguments(arguments);
        return alertDialogFragment;
    }

    /**
     * Factory method to create a new instance of {@link AlertDialogFragment} that displays a simple OK button.
     *
     * @param titleResId   the title string resource identifier.
     * @param messageResId the message string resource identifier.
     * @return a new instance of {@link AlertDialogFragment}.
     */
    public static AlertDialogFragment newInstance(int titleResId, int messageResId) {
        return newInstance(titleResId, messageResId, android.R.string.ok, 0);
    }

    /**
     * Factory method to create a new instance of {@link AlertDialogFragment} that displays a simple OK button.
     *
     * @param title   the title text.
     * @param message the message text.
     * @return a new instance of {@link AlertDialogFragment}.
     */
    public static AlertDialogFragment newInstance(String title, String message) {
        return newInstance(title, message, android.R.string.ok, 0);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (arguments.containsKey(KEY_TITLE_RES_ID)) {
            builder.setTitle(arguments.getInt(KEY_TITLE_RES_ID));
        } else if (arguments.containsKey(KEY_TITLE)) {
            builder.setTitle(arguments.getString(KEY_TITLE));
        }
        if (arguments.containsKey(KEY_MESSAGE_RES_ID)) {
            builder.setMessage(arguments.getInt(KEY_MESSAGE_RES_ID));
        } else if (arguments.containsKey(KEY_MESSAGE)) {
            builder.setTitle(arguments.getString(KEY_MESSAGE));
        }
        if (arguments.containsKey(KEY_POSITIVE_BUTTON_RES_ID)) {
            builder.setPositiveButton(arguments.getInt(KEY_POSITIVE_BUTTON_RES_ID), this);
        } else if (arguments.containsKey(KEY_POSITIVE_BUTTON)) {
            builder.setPositiveButton(arguments.getString(KEY_POSITIVE_BUTTON), this);
        }
        if (arguments.containsKey(KEY_NEGATIVE_BUTTON_RES_ID)) {
            builder.setNegativeButton(arguments.getInt(KEY_NEGATIVE_BUTTON_RES_ID), this);
        } else if (arguments.containsKey(KEY_NEGATIVE_BUTTON)) {
            builder.setNegativeButton(arguments.getString(KEY_NEGATIVE_BUTTON), this);
        }
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final Fragment targetFragment = getTargetFragment();
        if (targetFragment != null) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                targetFragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                targetFragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
            }
        }
    }
}
