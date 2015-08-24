package com.withkash.kash;

import java.util.HashMap;
import java.util.List;
import java.net.InetAddress;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONException;
import org.json.JSONObject;

public class KashPayment {
    protected String mPublishableKey;

    private MaterialDialog mDialog;
    private WebView mWebView;
    private Activity mActivity;
    private int mActivityOriginalOrientation;

    private String mAppEndpoint = "https://cdn.withkash.com/kash.js/1.0.0/android.html";
    private String mApiEndpoint = null;

    private KashPayment() {
        // prevent default constructor
    }

    public KashPayment(String publishableKey) {
        mPublishableKey = new String(publishableKey);
    }

    public static class Result {
        public String customer_id;
        public Institution institution;
        public String selected_account;

        public Result() {
            institution = new Institution();
        }
    }

    public static class Institution {
        public String institution_id;
        public String display_name;
        public String image;
        public String icon;
    }

    public static class ResultHandler {
        public void onComplete(Result result) {
        }

        public void onCancel() {
        }
    }

    public void show(final Activity activity, HashMap<String, String> options, final ResultHandler resultHandler) {
        if (mDialog == null || !mDialog.isShowing()) {
            if (options != null) {
                if (options.get("appEndpoint") != null) {
                    mAppEndpoint = options.get("appEndpoint");
                }

                if (options.get("apiEndpoint") != null) {
                    mApiEndpoint = options.get("apiEndpoint");
                }
            }

            mActivity = activity;

            mWebView = new WebView(mActivity);
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.clearCache(true);
            mWebView.clearHistory();

            // lock orientation while dialog is up
            mActivityOriginalOrientation = mActivity.getRequestedOrientation();
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

            final KashPayment kashPaymentDialog = this;
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);

                    String jsInitCode = "initializeKashJS({publishableKey:'" + mPublishableKey + "'";
                    if (mApiEndpoint != null) {
                        jsInitCode += ",apiEndpoint:'" + mApiEndpoint + "'";
                    }
                    jsInitCode += "})";

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        mWebView.evaluateJavascript(jsInitCode, null);
                    } else {
                        mWebView.loadUrl("javascript:" + jsInitCode);
                    }
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.startsWith("kash")) {
                        // extract data payload from 'kash protocol'
                        Uri uri = Uri.parse(url);
                        List<String> pathSegments = uri.getPathSegments();

                        JsToNativeArgs jsArgs = new JsToNativeArgs(pathSegments);

                        JSONObject resultJSON = jsArgs.getData();
                        if (resultJSON == null) {
                            return true;
                        }

                        // package result from JSON
                        Result result = new Result();
                        result.customer_id = resultJSON.optString("customer_id", null);
                        result.selected_account = resultJSON.optString("selected_account", null);

                        JSONObject institutionJSON = resultJSON.optJSONObject("institution");
                        if (institutionJSON != null) {
                            result.institution.institution_id = institutionJSON.optString("institution_id", null);
                            result.institution.display_name = institutionJSON.optString("display_name", null);
                            result.institution.image = institutionJSON.optString("image", null);
                            result.institution.icon = institutionJSON.optString("icon", null);
                        }

                        // hide UI
                        kashPaymentDialog.dismiss();

                        // invoke callback
                        if (resultHandler != null) {
                            if (result.customer_id != null) {
                                resultHandler.onComplete(result);
                            } else {
                                resultHandler.onCancel();
                            }
                        }

                        return true;
                    } else {
                        return false;
                    }
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    kashPaymentDialog.handleNetworkError();
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    kashPaymentDialog.handleNetworkError();
                }

                @Override
                public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                    super.onReceivedHttpError(view, request, errorResponse);
                    kashPaymentDialog.handleNetworkError();
                }
            });

            new InternetCheckTask(new InternetCheckTask.ResultHandler() {
                @Override
                public void onComplete(boolean internetAvailable) {
                    if (internetAvailable) {
                        // load webapp
                        mWebView.loadUrl(mAppEndpoint);

                        // load modal dialog
                        mDialog = new MaterialDialog.Builder(mActivity)
                                .customView(mWebView, false)
                                .show();
                    }
                    else {
                        kashPaymentDialog.handleNetworkError();
                    }
                }
            }).execute();
        }
    }

    public void dismiss() {
        if (mDialog != null) {
            mDialog.dismiss();
            mActivity.setRequestedOrientation(mActivityOriginalOrientation);
        }
    }

    private class JsToNativeArgs {
        private JSONObject mData;

        public JsToNativeArgs(List<String> pathSegments) {
            if (pathSegments.size() % 2 != 0) {
                return;
            }

            int index = 0;
            while (index < pathSegments.size()) {
                String param = pathSegments.get(index++);
                String value = pathSegments.get(index++);

                if (param.equals("data")) {
                    try {
                        mData = new JSONObject(value);
                    }
                    catch (JSONException e) {
                    }
                }
            }
        }

        public JSONObject getData() {
            return mData;
        }
    }

    private static class InternetCheckTask extends AsyncTask<Void,Void,Void> {
        private boolean mInternetAvailable = false;
        private ResultHandler callback;

        public InternetCheckTask(ResultHandler c) {
            callback = c;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            callback.onComplete(mInternetAvailable);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                InetAddress ipAddr = InetAddress.getByName("withkash.com");
                mInternetAvailable = !(ipAddr.equals(""));
            } catch (Exception e) {
                mInternetAvailable = false;
            }
            return null;
        }

        public static class ResultHandler {
            public void onComplete(boolean internetAvailable) {
            }
        }
    }

    private void handleNetworkError() {
        mWebView.setVisibility(View.INVISIBLE);
        this.dismiss();
        Toast.makeText(mWebView.getContext(),
                    "Direct debit services are currently unavailable. \n\nIs your network connection ok?",
                    Toast.LENGTH_LONG)
                .show();
    }
}
