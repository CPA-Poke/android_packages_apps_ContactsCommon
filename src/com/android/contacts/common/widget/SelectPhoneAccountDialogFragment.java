/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.contacts.common.widget;

import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.contacts.common.R;

import java.util.List;

/**
 * Dialog that allows the user to select a phone accounts for a given action. Optionally provides
 * the choice to set the phone account as default.
 */
public class SelectPhoneAccountDialogFragment extends DialogFragment {
    private boolean mCanSetDefault;
    private List<PhoneAccountHandle> mAccountHandles;
    private boolean mIsSelected;
    private boolean mIsDefaultChecked;
    private TelecomManager mTelecomManager;
    private SelectPhoneAccountListener mListener;

    /**
     * Shows the account selection dialog.
     * This is the preferred way to show this dialog.
     *
     * @param fragmentManager The fragment manager.
     * @param accountHandles The {@code PhoneAccountHandle}s available to select from.
     */
    public static void showAccountDialog(FragmentManager fragmentManager, boolean canSetDefault,
            List<PhoneAccountHandle> accountHandles, SelectPhoneAccountListener listener) {
        SelectPhoneAccountDialogFragment fragment =
                new SelectPhoneAccountDialogFragment(canSetDefault, accountHandles, listener);
        fragment.show(fragmentManager, "selectAccount");
    }

    public SelectPhoneAccountDialogFragment(boolean canSetDefault,
            List<PhoneAccountHandle> accountHandles, SelectPhoneAccountListener listener) {
        super();
        mCanSetDefault = canSetDefault;
        mAccountHandles = accountHandles;
        mListener = listener;
    }

    public interface SelectPhoneAccountListener {
        void onPhoneAccountSelected(PhoneAccountHandle selectedAccountHandle, boolean setDefault);
        void onDialogDismissed();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mIsSelected = false;
        mIsDefaultChecked = false;
        mTelecomManager =
                (TelecomManager) getActivity().getSystemService(Context.TELECOM_SERVICE);

        final DialogInterface.OnClickListener selectionListener =
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsSelected = true;
                PhoneAccountHandle selectedAccountHandle = mAccountHandles.get(which);
                mListener.onPhoneAccountSelected(selectedAccountHandle, mIsDefaultChecked);
            }
        };

        final CompoundButton.OnCheckedChangeListener checkListener =
                new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton check, boolean isChecked) {
                mIsDefaultChecked = isChecked;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ListAdapter selectAccountListAdapter = new SelectAccountListAdapter(
                builder.getContext(),
                R.layout.select_account_list_item,
                mAccountHandles);

        AlertDialog dialog = builder.setTitle(R.string.select_account_dialog_title)
                .setAdapter(selectAccountListAdapter, selectionListener)
                .create();

        if (mCanSetDefault) {
            // Generate custom checkbox view
            LinearLayout checkboxLayout = (LinearLayout) getActivity()
                    .getLayoutInflater()
                    .inflate(R.layout.default_account_checkbox, null);

            CheckBox cb =
                    (CheckBox) checkboxLayout.findViewById(R.id.default_account_checkbox_view);
            cb.setOnCheckedChangeListener(checkListener);

            dialog.getListView().addFooterView(checkboxLayout);
        }

        return dialog;
    }

    private class SelectAccountListAdapter extends ArrayAdapter<PhoneAccountHandle> {
        private int mResId;

        public SelectAccountListAdapter(
                Context context, int resource, List<PhoneAccountHandle> accountHandles) {
            super(context, resource, accountHandles);
            mResId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView;
            final ViewHolder holder;

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                holder.textView = (TextView) rowView.findViewById(R.id.text);
                holder.imageView = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            }
            else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            PhoneAccountHandle accountHandle = getItem(position);
            PhoneAccount account = mTelecomManager.getPhoneAccount(accountHandle);
            holder.textView.setText(account.getLabel());
            holder.imageView.setImageDrawable(account.createIconDrawable(getContext()));
            return rowView;
        }

        private class ViewHolder {
            TextView textView;
            ImageView imageView;
        }
    }

    @Override
    public void onPause() {
        if (!mIsSelected) {
            mListener.onDialogDismissed();
        }
        super.onPause();
    }
}
