package com.lcneves.cookme;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.util.JsonReader;
import android.view.Menu;
import android.view.MenuItem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.zip.GZIPInputStream;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class JSONHelper extends Activity {

    private static final String NAME = "name";
    private static final String INGREDIENTS = "ingredients";
    private static final String URL = "url";
    static final String recName="Name";
    static final String recIngredients="Ingredients";
    static final String recURL="URL";
    static final String recLength="Length";
    static final String recipesTable="Recipes";
    static String fileNameOld = "recipeitems-latest.json";
    static String fileNameNew = "recipeitems-edited.json";
    static String fileNameGz = "recipeitems-latest.json.gz";
    static File fileOld = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileNameOld);
    static File fileNew = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileNameNew);
    static File fileGz = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileNameGz);
    static String JSONUrl = "http://openrecipes.s3.amazonaws.com/recipeitems-latest.json.gz";
    private int lineCount = 0;
    Context context = JSONHelper.this;
    ProgressDialog mProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jsonhelper);

        downloadJSON(JSONUrl);
    }

/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.jsonhelper, menu);
        return true;
    }*/
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        *//*
        if (id == R.id.action_settings) {
            return true;
        }*//*
        return super.onOptionsItemSelected(item);
    }*/

    private void downloadJSON(String url) {
        mProgressDialog = new ProgressDialog(JSONHelper.this);
        mProgressDialog.setMessage("Downloading...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);

        final DownloadTask downloadTask = new DownloadTask(JSONHelper.this);
        downloadTask.execute(url);

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadTask.cancel(true);
            }
        });
    }

    private void unzipJSON(File fileIn, File fileOut) {
        mProgressDialog = new ProgressDialog(JSONHelper.this);
        mProgressDialog.setMessage("Decompressing...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        final UnzipTask unzipTask = new UnzipTask(JSONHelper.this);
        unzipTask.execute(fileIn, fileOut);
    }

    private void rebuildJSON(File fileIn, File fileOut) {
        mProgressDialog = new ProgressDialog(JSONHelper.this);
        mProgressDialog.setMessage("Reformatting database file...");
        mProgressDialog.setCancelable(false);

        final RebuildTask rebuildTask = new RebuildTask(JSONHelper.this);
        rebuildTask.execute(fileIn, fileOut);
    }

    private void parseJSON() {
        mProgressDialog = new ProgressDialog(JSONHelper.this);
        mProgressDialog.setMessage("Parsing "+lineCount+" recipes...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        final ParseTask parseTask = new ParseTask(JSONHelper.this);
        parseTask.execute();
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;
        double fileLength;
        int fileLengthInt;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            if(fileGz.exists())
                fileGz.delete();
            if(fileOld.exists())
                fileOld.delete();
            if(fileNew.exists())
                fileNew.delete();
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                fileLengthInt = connection.getContentLength();
                fileLength = fileLengthInt;

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(fileGz);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        if(fileGz.exists())
                            fileGz.delete();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total));
                        output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("Downloading "+(new DecimalFormat("#.##").format(fileLength/1048576))+" MB...");
            mProgressDialog.setMax(fileLengthInt);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null)
                Toast.makeText(context,"Download error: "+result, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(context,"File downloaded, unzipping...", Toast.LENGTH_SHORT).show();
            unzipJSON(fileGz, fileOld);
        }
    }

    private class UnzipTask extends AsyncTask<File, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public UnzipTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(File... fileInOut) {
            int sChunk = 8192;
            int length;
            byte[] buffer = new byte[sChunk];
            InputStream is = null;
            GZIPInputStream zis = null;
            BufferedOutputStream bos = null;

            try {
                is = new FileInputStream(fileInOut[0]);
                zis = new GZIPInputStream(new BufferedInputStream(is));
                bos = new BufferedOutputStream(new FileOutputStream(fileInOut[1]));
                long total = 0;
                long fileLength = fileInOut[0].length();

                while ((length = zis.read(buffer, 0, sChunk)) != -1) {
                    total += length;
                    publishProgress((int) (total * 100 / fileLength));
                    bos.write(buffer, 0, length);
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                try {
                    bos.close();
                    zis.close();
                    is.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result == null)
                rebuildJSON(fileOld, fileNew);
        }
    }

    private class RebuildTask extends AsyncTask<File, Void, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public RebuildTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(File... fileInOut) {
            if(fileInOut[1].exists())
                fileInOut[1].delete();

            FileInputStream fileStream = null;
            try {
                fileStream = new FileInputStream(fileInOut[0]);

                BufferedReader br = new BufferedReader(new InputStreamReader(fileStream));
                BufferedWriter bw = new BufferedWriter(new FileWriter(fileInOut[1]));
                String strLine;

                bw.write("[");

                while ((strLine = br.readLine()) != null)   {
                    bw.write(strLine + ",");
                    bw.newLine();
                    lineCount++;
                }

                bw.write("]");

                bw.close();
                br.close();
            } catch (FileNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result == null) {
                parseJSON();
            }
        }
    }

    private class ParseTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public ParseTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(final String... args) {
            DatabaseHelper database = new DatabaseHelper(getApplicationContext());
            database.recreateDatabase();

            SQLiteDatabase db = database.getWritableDatabase();
            db.beginTransaction();
            JsonReader jsonReader = null;
            String jsonName;
            String jsonIngredients;
            String jsonUrl;
            int lineProgress = 0;
            try {
                jsonReader = new JsonReader(new BufferedReader(new FileReader(fileNew)));
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    jsonName = null;
                    jsonIngredients = null;
                    jsonUrl = null;
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        String name = jsonReader.nextName();
                        if (name.equals(NAME)) {
                            jsonName = jsonReader.nextString().replace("&amp;", "&");
                        } else if (name.equals(INGREDIENTS)) {
                            jsonIngredients = jsonReader.nextString().replace("&amp;", "&");
                        } else if (name.equals(URL)) {
                            jsonUrl = jsonReader.nextString();
                        } else {
                            jsonReader.skipValue();
                        }
                    }
                    lineProgress++;
                    if(lineProgress % 1000 == 0)
                        publishProgress((int) (lineProgress));
                    jsonReader.endObject();
                    ContentValues cv=new ContentValues();
                    cv.put(recName, jsonName);
                    cv.put(recIngredients, jsonIngredients);
                    cv.put(recURL, jsonUrl);
                    cv.put(recLength, jsonIngredients.length());
                    db.insertOrThrow(recipesTable, null, cv);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                db.setTransactionSuccessful();
                db.endTransaction();
                db.close();
                try {
                    jsonReader.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(lineCount);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result == null) {
                if(fileGz.exists())
                    fileGz.delete();
                if(fileOld.exists())
                    fileOld.delete();
                if(fileNew.exists())
                    fileNew.delete();
                Intent intent = new Intent(JSONHelper.this, MainActivity.class);
                startActivity(intent);
            }
        }
    }
}
