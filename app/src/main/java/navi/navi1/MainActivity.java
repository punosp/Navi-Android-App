package navi.navi1;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikhaellopez.circularimageview.CircularImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Context ctx=this;

    private static final int REQUEST_CODE = 1234;
    private ListView wordsList;
    private TextToSpeech textToSpeech;

    LinearLayout chatLayout, answerLayout;
    ScrollView scrollView;
    Button speakButton, sendButton;
    EditText query;
    final String API_URL = "http://35.154.232.21:1337/api/v1/query";
    final String FAIL_URL="http://192.168.43.15/navi/pages/fail/fail.html";
    final String SORRY_TEXT="Sorry, I don't have any knowledge about it for now.. Please ask another";
    final String Welcome_Text="Hi there, I am Navi. How may I help you ?";
    int chatViewIndex=-1;
    boolean questionReAsk=false;
    ArrayList<View> chatBlocks = new ArrayList<>();
    View presentQuestionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        chatLayout = (LinearLayout)findViewById(R.id.messageFromServer);
       // answerLayout = (LinearLayout)findViewById(R.id.answerOptions);

        scrollView=(ScrollView)findViewById(R.id.scrollView);

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollView.post(new Runnable() {
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        textToSpeech= new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result=textToSpeech.setLanguage(Locale.US);
                    if(result==TextToSpeech.LANG_MISSING_DATA || result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Toast.makeText(getApplicationContext(), "Language Not Supported", Toast.LENGTH_SHORT).show();
                    } else{
                        //startVoiceRecognitionActivity();
                    }
                }
                else
                    Toast.makeText(getApplicationContext(), "Initialisation Failed", Toast.LENGTH_SHORT).show();
            }
        });

        addMessageFromServer("-1", Welcome_Text);

        speakButton = (Button) findViewById(R.id.speakButton);
        sendButton = (Button) findViewById(R.id.send);
        query = (EditText)findViewById(R.id.edit_query);


        //wordsList = (ListView) findViewById(R.id.list);

        // Disable button if no recognition service is present
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0)
        {
            speakButton.setEnabled(false);
            speakButton.setText("Recognizer not present");
        }
        //getOrderInfoFromVolley();

    }
    public void speakButtonClicked(View v)
    {
        textToSpeech.stop();
        startVoiceRecognitionActivity();
    }
    public void sendButtonClicked(View v) {
        String q = query.getText().toString();
        addAnswerToView(q);
        BackGround b = new BackGround();
        b.execute(q);
    }
    /**
     * Fire an intent to start the voice recognition activity.
     */
    private void startVoiceRecognitionActivity()
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice recognition");
        startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * Handle the results from the voice recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK)
        {
            // Populate the wordsList with the String values the recognition engine thought it heard
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            addAnswerToView(matches.get(0));
            BackGround b = new BackGround();
            b.execute(matches.get(0));
//            wordsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
//                    matches));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    class BackGround extends AsyncTask<String, String, String[]> {



        @Override
        protected String[] doInBackground(String... params) {
            String query = params[0];
            String data="";
            int tmp;

            String response[] = new String[2];

            try {
                URL url = new URL(API_URL);
                String urlParams = "query="+query+"&user="+410;

                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                // httpURLConnection.setDoOutput(true);
                httpURLConnection.setReadTimeout(15000);
                httpURLConnection.setConnectTimeout(15000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                OutputStream os = httpURLConnection.getOutputStream();
                os.write(urlParams.getBytes());
                os.flush();
                os.close();
                InputStream is = httpURLConnection.getInputStream();
                while((tmp=is.read())!=-1){
                    data+= (char)tmp;
                }

                is.close();
                httpURLConnection.disconnect();
                response[0] = "1";// 1 for success
                response[1] = data;
                return response;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                response[0] = "0";// 0 for error
                response[1] = "error";
                return response;
            } catch (Exception e) {
                e.printStackTrace();
                response[0] = "0";// 0 for error
                response[1] = "error";
                return response;
            }
        }

        @Override
        protected void onPostExecute(String s[]) {
            String resp;
            if (s.length!=0) {
                String err = null;
                if(s[0].equals("0")) {
                    addMessageFromServer("0", SORRY_TEXT);

                }
                else {
                    try {
                        JSONObject root = new JSONObject(s[1]);
                        JSONObject data = root.getJSONObject("data");
                        if(data.getString("code").equalsIgnoreCase("11")) {
                            addMessageFromServer("2", data.getString("data"));
                        }
                        else {
                            JSONObject data1 = data.getJSONObject("data");
                            String match = data1.getString("match");
                            JSONArray records = data1.getJSONArray("records");
                            int count = Integer.parseInt(data1.getString("count"));
                            if (match.equalsIgnoreCase("full")) {
                                if (count == 1) {
                                    JSONObject rec = records.getJSONObject(0);
                                    addMessageFromServer("2", rec.getString("answer"));
                                } else if (count > 1) {
                                    String ans = "I found ";
                                    for (int i = 0; i < count; i++) {
                                        JSONObject rec = records.getJSONObject(i);
                                        String name = rec.getString("name");
                                        if (i < count - 1)
                                            ans += name + ", ";
                                        else
                                            ans += name;
                                    }
                                    addMessageFromServer("2", ans);
                                } else {
                                    addMessageFromServer("0", SORRY_TEXT);
                                }
                            } else {
                                String ans = "Are you looking for ";
                                for (int i = 0; i < count; i++) {
                                    JSONObject rec = records.getJSONObject(i);
                                    String name = rec.getString("name");
                                    if (i < count - 1)
                                        ans += name + "or ";
                                    else
                                        ans += name + " ?";
                                }
                                addMessageFromServer("1", ans);
                            }
                            // Toast.makeText(ctx, a, Toast.LENGTH_LONG).show();
                        }

                    } catch (JSONException e) {
                        addMessageFromServer("0", SORRY_TEXT);

                    }
                }



            }
        }
    }

    private void addMessageFromServer(String code, String response) {
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View block = layoutInflater.inflate(R.layout.chat_block, null);

        TextView question=(TextView)block.findViewById(R.id.question);

        if(code.equalsIgnoreCase("-1")) {
            response="Hi there, I am Navi.\n" +
                    "How may I help you ?";
            question.setText(response);


        }
        else {
            question.setText(response);

        }

        addToChatView(block);
        textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null);

        //TODO: change to real question Index

        //presentQuestionView=block;
        //chatLayout.addView(block);
    }
    private void addToChatView(View block){
        chatViewIndex++;
        block.setTag(R.string.tag_index, String.valueOf(chatViewIndex));
        chatLayout.addView(block);
        //chatBlocks.add(block);
    }

    private void addAnswerToView(String answer) {
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View block = layoutInflater.inflate(R.layout.chat_answer_block, null);
        LinearLayout parentLayout=(LinearLayout)block.findViewById(R.id.parentLayout);
        parentLayout.setGravity(Gravity.END);
        TextView answerText= (TextView) block.findViewById(R.id.answer);
        answerText.setText(answer);
        //textToSpeech.speak(answer, TextToSpeech.QUEUE_FLUSH, null);
        addToChatView(block);
        //chatLayout.addView(block);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
        textToSpeech.stop();
        return;
    }
}


