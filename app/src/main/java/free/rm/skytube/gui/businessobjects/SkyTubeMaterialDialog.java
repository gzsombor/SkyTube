/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.checkbox.DialogCheckboxExtKt;
import com.afollestad.materialdialogs.customview.DialogCustomViewExtKt;
import com.afollestad.materialdialogs.list.DialogListExtKt;

import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;

import free.rm.skytube.R;
import kotlin.jvm.functions.Function1;
import kotlin.Unit;

/**
 * Material dialog for SkyTube in which the theme colors are pre-set for convenience.
 */
public class SkyTubeMaterialDialog {

	public interface DialogCallback extends Function1<MaterialDialog,Unit> {}
	public interface CheckboxCallback extends Function1<Boolean,Unit> {}

	MaterialDialog dialog;
	Integer title;
	Integer content;
	CharSequence contentMsg;
	Integer positiveText = R.string.ok;
	Integer negativeText = R.string.cancel;
	String positiveTextStr = null;
	String negativeTextStr = null;
	Integer neutralText;
	DialogCallback positiveCallback;
	DialogCallback neutralCallback;
	DialogCallback negativeCallback = new DialogCallback() {
		@Override
		public Unit invoke(MaterialDialog materialDialog) {
			dialog.dismiss();
			return null;
		}
	};

	public SkyTubeMaterialDialog(@NonNull Context context) {
		dialog = new MaterialDialog(context, MaterialDialog.getDEFAULT_BEHAVIOR());
	}

	public SkyTubeMaterialDialog content(@StringRes Integer content) {
		this.content = content;
		return this;
	}

	public SkyTubeMaterialDialog noAutoDismiss() {
		this.dialog.noAutoDismiss();
		return this;
	}

	public SkyTubeMaterialDialog title(@StringRes Integer title) {
		this.title = title;
		return this;
	}

	public SkyTubeMaterialDialog content(@NonNull CharSequence contentMsg) {
		this.contentMsg = contentMsg;
		return this;
	}
	public SkyTubeMaterialDialog cancelable(boolean cancelable) {
		this.dialog.setCancelable(cancelable);
		return this;
	}

	public SkyTubeMaterialDialog negativeText(@StringRes Integer negativeText) {
		this.negativeText = negativeText;
		return this;
	}
	public SkyTubeMaterialDialog positiveText(@StringRes Integer positiveText) {
		this.positiveText = positiveText;
		return this;
	}
	public SkyTubeMaterialDialog negativeText(String negativeText) {
		this.negativeTextStr = negativeText;
		return this;
	}
	public SkyTubeMaterialDialog positiveText(String positiveText) {
		this.positiveTextStr = positiveText;
		return this;
	}
	public SkyTubeMaterialDialog neutralText(@StringRes Integer neutralText) {
		this.neutralText = neutralText;
		return this;
	}
	public SkyTubeMaterialDialog onPositive(@NonNull DialogCallback callback) {
		this.positiveCallback = callback;
		return this;
	}
	public SkyTubeMaterialDialog onNegative(@NonNull DialogCallback callback) {
		this.negativeCallback = callback;
		return this;
	}
	public SkyTubeMaterialDialog onNeutral(@NonNull DialogCallback callback) {
		this.neutralCallback = callback;
		return this;
	}
	public SkyTubeMaterialDialog adapter(@NonNull Adapter adapter, LayoutManager layoutManager) {
		DialogListExtKt.customListAdapter(this.dialog, adapter, layoutManager);
		return this;
	}
	public View getCustomView() {
		return DialogCustomViewExtKt.getCustomView(dialog);
	}

	public SkyTubeMaterialDialog customView(@LayoutRes int viewRes,
											View view ,
											Boolean scrollable,
											Boolean noVerticalPadding,
											Boolean horizontalPadding,
											Boolean dialogWrapContent) {
		DialogCustomViewExtKt.customView(this.dialog, viewRes, view, scrollable, noVerticalPadding, horizontalPadding, dialogWrapContent);
		return this;
	}

	public SkyTubeMaterialDialog checkBoxPromptRes(@StringRes int res, String text, boolean isCheckedDefault, CheckboxCallback onToggle) {
		DialogCheckboxExtKt.checkBoxPrompt(dialog, res, text, isCheckedDefault, onToggle);
		return this;
	}
	public MaterialDialog show() {
		dialog.positiveButton(positiveText, positiveTextStr, positiveCallback);
		dialog.negativeButton(negativeText, negativeTextStr, negativeCallback);
		if (neutralText != null) {
			dialog.neutralButton(neutralText, null, neutralCallback);
		}
		dialog.message(content, contentMsg, null);
		dialog.title(title, null);
		dialog.show();
		return dialog;
	}
}
