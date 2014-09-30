package com.lcneves.cookme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;


public class RecipeViewer extends Activity {

    WebView webview;
    String url;
    String previousActivity;
    private ShareActionProvider mShareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.recipe_viewer);
        Intent intent = getIntent();
        url = intent.getStringExtra("com.lcneves.cookme.URL");
        previousActivity = intent.getStringExtra("com.lcneves.cookme.ACTIVITY");
        webview = (WebView) findViewById(R.id.webView);

        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);

        final Activity activity = this;
        webview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                // Activities and WebViews measure progress with different scales.
                // The progress meter will automatically disappear when we reach 100%
                activity.setProgress(progress * 1000);
            }
        });

        webview.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(activity, "Loading error: " + description, Toast.LENGTH_LONG).show();
            }
        });
        Log.d("com.lcneves.cookme.RecipeViewer", url);
        webview.loadUrl(url);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.recipe_viewer, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, url);
        sendIntent.setType("text/plain");
        mShareActionProvider.setShareIntent(sendIntent);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
/*        if (id == R.id.action_settings) {
            return true;
        }*/
        if(id == android.R.id.home) {
            Log.d("com.lcneves.cookme.RecipeViewer", "previousActivity = "+previousActivity);
            if(previousActivity.equals("display")) {
                Intent intent = new Intent(this, DisplayResults.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
            if(previousActivity.equals("simple")) {
                Intent intent = new Intent(this, SearchSimple.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void clickAboutMenuViewer(MenuItem menu) {
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);
        TextView par1 = (TextView) messageView.findViewById(R.id.aboutPar1);
        TextView par4 = (TextView) messageView.findViewById(R.id.aboutPar4);
        Log.d("com.lcneves.cookme.MainActivity", "Par4 = "+par4.toString());
        Linkify.addLinks(par1, Linkify.WEB_URLS);
        Linkify.addLinks(par4, Linkify.WEB_URLS);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }
}
