package com.lcneves.cookme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Toast;

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

public class JSONHelper extends Activity {

    final String recName = DatabaseHelper.recName;
    final String recIngredients = DatabaseHelper.recIngredients;
    final String recURL = DatabaseHelper.recURL;
    final String recipesTable = DatabaseHelper.recipesTable;
    static final String fileNameOld = "recipeitems-latest.json";
    static final String fileNameNew = "recipeitems-edited.json";
    static final String fileNameGz = "recipeitems-latest.json.gz";
    static File fileDir;
    static File fileOld;
    static File fileNew;
    static File fileGz;
    static String JSONUrl = "http://openrecipes.s3.amazonaws.com/recipeitems-latest.json.gz";
    private int lineCount = 0;
    Context context = JSONHelper.this;
    ProgressDialog mProgressDialog;
    DatabaseHelper database = new DatabaseHelper(this);
    InputStream input = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jsonhelper);
        final int MIN_FREE_SPACE = 314572800; // During tests, this class used up to 250+ MB.
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            if(context.getExternalFilesDir(null).getFreeSpace() > MIN_FREE_SPACE) {
                fileDir = context.getExternalFilesDir(null);
            } else {
                if(context.getFilesDir().getFreeSpace() > MIN_FREE_SPACE) {
                    fileDir = context.getFilesDir();
                } else {
                    FreeSpaceDialogFragment dialogFreeSpace = new FreeSpaceDialogFragment();
                    dialogFreeSpace.show(getFragmentManager(), "tag");
                }
            }
        } else {
            if(context.getFilesDir().getFreeSpace() > MIN_FREE_SPACE) {
                fileDir = context.getFilesDir();
            } else {
                FreeSpaceDialogFragment dialogFreeSpace = new FreeSpaceDialogFragment();
                dialogFreeSpace.show(getFragmentManager(), "tag");
            }
        }
        Log.d("com.lcneves.cookme.JSONHelper", "fileDir = "+fileDir.toString());
        fileOld = new File(fileDir, fileNameOld);
        fileNew = new File(fileDir, fileNameNew);
        fileGz = new File(fileDir, fileNameGz);

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm.getActiveNetworkInfo() != null) {
            if(cm.getActiveNetworkInfo().isConnected()) {
                downloadJSON(JSONUrl);
            }
        } else {
            ConnectivityDialogFragment dialogConnectivity = new ConnectivityDialogFragment();
            dialogConnectivity.show(getFragmentManager(), "tag");
        }
    }

    private void downloadJSON(String url) {
        mProgressDialog = new ProgressDialog(JSONHelper.this);
        mProgressDialog.setMessage("Cleaning old database...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        final DownloadTask downloadTask = new DownloadTask(JSONHelper.this);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                downloadTask.cancel(true);
            }
        });
        downloadTask.execute(url);
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
            OutputStream output = null;
            HttpURLConnection connection = null;
            database.recreateDatabase();
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
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }
                fileLengthInt = connection.getContentLength();
                fileLength = fileLengthInt;

                input = connection.getInputStream();
                output = new FileOutputStream(fileGz);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    if (isCancelled()) {
                        input.close();
                        if(fileGz.exists())
                            fileGz.delete();
                        database.dropRecipes();
                        Intent intent = new Intent(JSONHelper.this, MainActivity.class);
                        startActivity(intent);
                        return null;
                    }
                    total += count;
                    if (fileLength > 0)
                        publishProgress((int) (total/1024));
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
            mProgressDialog.setMax(fileLengthInt/1024);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null) {
                Toast.makeText(context,"Download error: "+result, Toast.LENGTH_LONG).show();
                if(fileGz.exists())
                    fileGz.delete();
                database.dropRecipes();
                Intent intent = new Intent(JSONHelper.this, MainActivity.class);
                startActivity(intent);
            } else {
                unzipJSON(fileGz, fileOld);
            }
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
                if(fileGz.exists())
                    fileGz.delete();
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

                bw.write("{}]");
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
                if(fileOld.exists())
                    fileOld.delete();
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
            SQLiteDatabase db = database.getWritableDatabase();
            db.beginTransaction();
            JsonReader jsonReader = null;
            String jsonName;
            String jsonIngredients;
            String jsonUrl;
            final String NAME = "name";
            final String INGREDIENTS = "ingredients";
            final String URL = "url";
            int lineProgress = 0;
            long oldTime = System.nanoTime();
            String comma = ",";
            SQLiteStatement st = db.compileStatement("INSERT INTO "+recipesTable+" ("+recName+comma+recIngredients+comma+recURL+") VALUES (?,?,?);");

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
                    if(System.nanoTime() - oldTime > 1e9) { // update every second
                        oldTime = System.nanoTime();
                        publishProgress((int) (lineProgress));
                    }
                    jsonReader.endObject();
                    if(jsonName != null && jsonIngredients != null && jsonUrl != null) {
                        st.bindString(1, jsonName);
                        st.bindString(2, jsonIngredients);
                        st.bindString(3, jsonUrl);
                        st.executeInsert();
                        st.clearBindings();
                    }
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

    public static class FreeSpaceDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("This device does not have enough free space to import the database. Please free at least 300 MB and try again.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            startActivity(intent);
                        }
                    });
            return builder.create();
        }
    }

    public static class ConnectivityDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Please ensure that this device is connected to the internet.")
                    .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            new Handler().post(new Runnable() {

                                @Override
                                public void run() {
                                    Intent intent = getActivity().getIntent();
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    getActivity().overridePendingTransition(0, 0);
                                    getActivity().finish();

                                    getActivity().overridePendingTransition(0, 0);
                                    startActivity(intent);
                                }
                            });
                        }
                    })
                    .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            startActivity(intent);
                        }
                    });
            return builder.create();
        }
    }
}