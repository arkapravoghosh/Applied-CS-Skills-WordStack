/* Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.engedu.wordstack;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.WorkSource;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    private static Random random = new Random();
    private int WORD_LENGTH;
    public static final int LIGHT_BLUE = Color.rgb(176, 200, 255);
    public static final int LIGHT_GREEN = Color.rgb(200, 255, 200);
    private HashMap<Integer, ArrayList<String>> words = new HashMap<>();
    private HashMap<Integer, HashSet<String>> wordsSet = new HashMap<>();
    private Stack<LetterTile> placedTiles = new Stack<>();
    private StackedLayout stackedLayout;
    private String word1, word2, foundWord1, foundWord2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AssetManager assetManager = getAssets();
        try { // store the dictionary words in the words array
            InputStream inputStream = assetManager.open("words.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            for(int i = 3; i <= 6; ++i) {
                words.put(i, new ArrayList<String>());
                wordsSet.put(i, new HashSet<String>());
            }
            while((line = in.readLine()) != null) {
                String word = line.trim();
                if(word.length() >= 3 && word.length() <= 6) {
                    words.get(word.length()).add(word);
                    wordsSet.get(word.length()).add(word);
                }
            }
        } catch (IOException e) {
            Toast toast = Toast.makeText(this, "Could not load dictionary", Toast.LENGTH_LONG);
            toast.show();
        }
        LinearLayout verticalLayout = (LinearLayout) findViewById(R.id.vertical_layout);
        stackedLayout = new StackedLayout(this);
        verticalLayout.addView(stackedLayout, 3);
        findViewById(R.id.button).setEnabled(false);

        View word1LinearLayout = findViewById(R.id.word1);
        //word1LinearLayout.setOnTouchListener(new TouchListener());
        word1LinearLayout.setOnDragListener(new DragListener());
        View word2LinearLayout = findViewById(R.id.word2);
        //word2LinearLayout.setOnTouchListener(new TouchListener());
        word2LinearLayout.setOnDragListener(new DragListener());
    }

    private class TouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN && !stackedLayout.empty()) {
                LetterTile tile = (LetterTile) stackedLayout.peek();
                tile.moveToViewGroup((ViewGroup) v);
                placedTiles.push(tile);
                if (stackedLayout.empty()) {
                    TextView messageBox = (TextView) findViewById(R.id.message_box);
                    messageBox.setText(word1 + " " + word2);
                }
                return true;
            }
            return false;
        }
    }

    private class DragListener implements View.OnDragListener {

        public boolean onDrag(View v, DragEvent event) {
            int action = event.getAction();
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    v.setBackgroundColor(LIGHT_BLUE);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundColor(LIGHT_GREEN);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundColor(LIGHT_BLUE);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundColor(Color.WHITE);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DROP:
                    // Dropped, reassign Tile to the target Layout
                    LetterTile tile = (LetterTile) event.getLocalState();
                    tile.moveToViewGroup((ViewGroup) v);
                    placedTiles.push(tile);
                    if(v == findViewById(R.id.word1))
                        foundWord1 += tile.getLetter();
                    else
                        foundWord2 += tile.getLetter();
                    findViewById(R.id.button).setEnabled(true);
                    if(stackedLayout.empty()) {
                        if(wordsSet.get(WORD_LENGTH).contains(foundWord1) && wordsSet.get(WORD_LENGTH).contains(foundWord2)) {
                            word1 = foundWord1;
                            word2 = foundWord2;
                        }
                        TextView messageBox = (TextView) findViewById(R.id.message_box);
                        messageBox.setText(word1 + "  " + word2);
                        findViewById(R.id.button).setEnabled(false);
                    }
                    return true;
            }
            return false;
        }
    }

    public boolean onStartGame(View view) {
        LinearLayout word1LinearLayout = findViewById(R.id.word1);
        LinearLayout word2LinearLayout = findViewById(R.id.word2);
        word1LinearLayout.removeAllViews();
        word2LinearLayout.removeAllViews();
        stackedLayout.clear();
        while(!placedTiles.empty()) placedTiles.pop();
        foundWord1 = foundWord2 = "";
        WORD_LENGTH = random.nextInt(4) + 3;
        int word1Run = 0, word2Run = 0;

        TextView messageBox = (TextView) findViewById(R.id.message_box);
        messageBox.setText("Game started" + " | Drag the letters to the below boxes to form two " + WORD_LENGTH + "-lettered words");
        word1 = words.get(WORD_LENGTH).get(random.nextInt(words.get(WORD_LENGTH).size()));
        word2 = words.get(WORD_LENGTH).get(random.nextInt(words.get(WORD_LENGTH).size()));
        StringBuilder word = new StringBuilder(WORD_LENGTH*2);
        int i = 0, j = 0;
        while(i < WORD_LENGTH && j < WORD_LENGTH) {
            if(word1Run == 2) {
                word.append(word2.charAt(j++));
                word1Run = 0;
                word2Run = 1;
                continue;
            }
            if(word2Run == 2) {
                word.append(word1.charAt(i++));
                word1Run = 1;
                word2Run = 0;
                continue;
            }
            if(random.nextInt(2) == 0) {
                word.append(word1.charAt(i++));
                ++word1Run;
            } else {
                word.append(word2.charAt(j++));
                ++word2Run;
            }
        }
        while(i < WORD_LENGTH) {
            word.append(word1.charAt(i++));
        }
        while(j < WORD_LENGTH) {
            word.append(word2.charAt(j++));
        }
        LetterTile letterTile;
        for(i = word.length()-1; i >= 0; --i) {
            letterTile = new LetterTile(this, word.charAt(i));
            stackedLayout.push(letterTile);
        }
        return true;
    }

    public boolean onUndo(View view) {
        if(!placedTiles.empty()) {
            ViewGroup v = (ViewGroup)placedTiles.peek().getParent();
            placedTiles.pop().moveToViewGroup(stackedLayout);
            if(v == findViewById(R.id.word1))
                foundWord1 = foundWord1.substring(0, foundWord1.length()-1);
            else
                foundWord2 = foundWord2.substring(0, foundWord2.length()-1);
            if(placedTiles.empty())
                findViewById(R.id.button).setEnabled(false);
        }
        return true;
    }
}
