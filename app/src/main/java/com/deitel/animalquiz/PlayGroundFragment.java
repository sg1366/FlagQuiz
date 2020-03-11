/*
Sina Gharavi
*/

// PlayGroundFragment.java
// Contains the Animal Quiz logic

package com.deitel.animalquiz;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.deitel.animalquiz.MainActivity;
import com.deitel.animalquiz.PlayGroundActivity;
import com.deitel.animalquiz.R;

public class PlayGroundFragment extends Fragment {
   // String used when logging error messages
   private static final String TAG = "AnimalQuiz Activity";

   private static final int ANIMALS_IN_QUIZ = 10;

   private List<String> fileNameList; // animal file names
   private List<String> quizCountriesList; // countries in current quiz
   private Set<String> animalTypesSet; // animal types in current quiz
   private String correctAnswer; // correct country for the current animal
   private int totalGuesses; // number of guesses made
   private int correctAnswers; // number of correct guesses
   private int guessRows; // number of rows displaying guess Buttons
   private SecureRandom random; // used to randomize the quiz
   private Handler handler; // used to delay loading next animal
   private Animation shakeAnimation; // animation for incorrect guess

   private LinearLayout quizLinearLayout; // layout that contains the quiz
   private TextView questionNumberTextView; // shows current question #
   private ImageView animalImageView; // displays a animal
   private LinearLayout[] guessLinearLayouts; // rows of answer Buttons
   private TextView answerTextView; // displays correct answer

   public static boolean soundOptions;

   // configures the PlayGroundFragment when its View is created
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
      super.onCreateView(inflater, container, savedInstanceState);
      View view =
         inflater.inflate(R.layout.fragment_playground, container, false);

      fileNameList = new ArrayList<>();
      quizCountriesList = new ArrayList<>();
      random = new SecureRandom();
      handler = new Handler();

      // load the shake animation that's used for incorrect answers
      shakeAnimation = AnimationUtils.loadAnimation(getActivity(),
         R.anim.incorrect_shake);
      shakeAnimation.setRepeatCount(3); // animation repeats 3 times

      // get references to GUI components
      quizLinearLayout =
         (LinearLayout) view.findViewById(R.id.quizLinearLayout);
      questionNumberTextView =
         (TextView) view.findViewById(R.id.questionNumberTextView);
      animalImageView = (ImageView) view.findViewById(R.id.animalImageView);
      guessLinearLayouts = new LinearLayout[4];
      guessLinearLayouts[0] =
         (LinearLayout) view.findViewById(R.id.row1LinearLayout);
      guessLinearLayouts[1] =
         (LinearLayout) view.findViewById(R.id.row2LinearLayout);
      guessLinearLayouts[2] =
         (LinearLayout) view.findViewById(R.id.row3LinearLayout);
      guessLinearLayouts[3] =
         (LinearLayout) view.findViewById(R.id.row4LinearLayout);
      answerTextView = (TextView) view.findViewById(R.id.answerTextView);

      // configure listeners for the guess Buttons
      for (LinearLayout row : guessLinearLayouts) {
         for (int column = 0; column < row.getChildCount(); column++) {
            Button button = (Button) row.getChildAt(column);
            button.setOnClickListener(guessButtonListener);
         }
      }

      // set questionNumberTextView's text
      questionNumberTextView.setText(
         getString(R.string.question, 1, ANIMALS_IN_QUIZ));
      return view; // return the fragment's view for display
   }

   // update guessRows based on value in SharedPreferences
   public void updateGuessRows(SharedPreferences sharedPreferences) {
      // get the number of guess buttons that should be displayed
      String choices =
         sharedPreferences.getString(PlayGroundActivity.CHOICES, null);
      guessRows = Integer.parseInt(choices) / 2;

      // hide all quess button LinearLayouts
      for (LinearLayout layout : guessLinearLayouts)
         layout.setVisibility(View.GONE);

      // display appropriate guess button LinearLayouts
      for (int row = 0; row < guessRows; row++)
         guessLinearLayouts[row].setVisibility(View.VISIBLE);
   }

   // update sound options
   public void updateSoundOptions(SharedPreferences sharedPreferences) {
      soundOptions = sharedPreferences.getBoolean(PlayGroundActivity.SOUNDOPTION, true);
   }

   // update animal types for quiz based on values in SharedPreferences
   public void updateAnimalTypes(SharedPreferences sharedPreferences) {
      animalTypesSet =
         sharedPreferences.getStringSet(PlayGroundActivity.ANIMALTYPES, null);
   }

   // set up and start the next quiz
   public void resetQuiz() {
      // use AssetManager to get image file names for enabled animal types
      AssetManager assets = getActivity().getAssets();
      fileNameList.clear(); // empty list of image file names

      try {
         // loop through each animal type
         for (String animalType : animalTypesSet) {
            // get a list of all animal image files in this animal type
            String[] paths = assets.list(animalType);

            for (String path : paths)
               fileNameList.add(path.replace(".png", ""));
         }
      }
      catch (IOException exception) {
         Log.e(TAG, "Error loading image file names", exception);
      }

      correctAnswers = 0; // reset the number of correct answers made
      totalGuesses = 0; // reset the total number of guesses the user made
      quizCountriesList.clear(); // clear prior list of quiz countries

      int animalCounter = 1;
      int numberOfAnimals = fileNameList.size();

      // add ANIMALS_IN_QUIZ random file names to the quizCountriesList
      while (animalCounter <= ANIMALS_IN_QUIZ) {
         int randomIndex = random.nextInt(numberOfAnimals);

         // get the random file name
         String filename = fileNameList.get(randomIndex);

         // if the animal type is enabled and it hasn't already been chosen
         if (!quizCountriesList.contains(filename)) {
            quizCountriesList.add(filename); // add the file to the list
            ++animalCounter;
         }
      }

      loadNextAnimal(); // start the quiz by loading the first animal
   }

   // after the user guesses a correct animal, load the next animal
   private void loadNextAnimal() {
      // get file name of the next animal and remove it from the list
      String nextImage = quizCountriesList.remove(0);
      correctAnswer = nextImage; // update the correct answer
      answerTextView.setText(""); // clear answerTextView

      // display current question number
      questionNumberTextView.setText(getString(
         R.string.question, (correctAnswers + 1), ANIMALS_IN_QUIZ));

      // extract the animal type from the next image's name
      String animalType = nextImage.substring(0, nextImage.indexOf('-'));

      // use AssetManager to load next image from assets folder
      AssetManager assets = getActivity().getAssets();

      // get an InputStream to the asset representing the next animal
      // and try to use the InputStream
      try (InputStream stream =
              assets.open(animalType + "/" + nextImage + ".png")) {
         // load the asset as a Drawable and display on the animalImageView
         Drawable animal = Drawable.createFromStream(stream, nextImage);
         animalImageView.setImageDrawable(animal);

         animate(false); // animate the animal onto the screen
      }
      catch (IOException exception) {
         Log.e(TAG, "Error loading " + nextImage, exception);
      }

      Collections.shuffle(fileNameList); // shuffle file names

      // put the correct answer at the end of fileNameList
      int correct = fileNameList.indexOf(correctAnswer);
      fileNameList.add(fileNameList.remove(correct));

      // add 2, 4, 6 or 8 guess Buttons based on the value of guessRows
      for (int row = 0; row < guessRows; row++) {
         // place Buttons in currentTableRow
         for (int column = 0;
              column < guessLinearLayouts[row].getChildCount();
              column++) {
            // get reference to Button to configure
            Button newGuessButton =
               (Button) guessLinearLayouts[row].getChildAt(column);
            newGuessButton.setEnabled(true);

            // get country name and set it as newGuessButton's text
            String filename = fileNameList.get((row * 2) + column);
            newGuessButton.setText(getCountryName(filename));
         }
      }

      // randomly replace one Button with the correct answer
      int row = random.nextInt(guessRows); // pick random row
      int column = random.nextInt(2); // pick random column
      LinearLayout randomRow = guessLinearLayouts[row]; // get the row
      String countryName = getCountryName(correctAnswer);
      ((Button) randomRow.getChildAt(column)).setText(countryName);
   }

   // parses the country animal file name and returns the country name
   private String getCountryName(String name) {
      return name.substring(name.indexOf('-') + 1).replace('_', ' ');
   }

   // animates the entire quizLinearLayout on or off screen
   private void animate(boolean animateOut) {
      // prevent animation into the the UI for the first animal
      if (correctAnswers == 0)
         return;

      // calculate center x and center y
      int centerX = (quizLinearLayout.getLeft() +
         quizLinearLayout.getRight()) / 2; // calculate center x
      int centerY = (quizLinearLayout.getTop() +
         quizLinearLayout.getBottom()) / 2; // calculate center y

      // calculate animation radius
      int radius = Math.max(quizLinearLayout.getWidth(),
         quizLinearLayout.getHeight());

      Animator animator;

      // if the quizLinearLayout should animate out rather than in
      if (animateOut) {
         // create circular reveal animation
         animator = ViewAnimationUtils.createCircularReveal(
            quizLinearLayout, centerX, centerY, radius, 0);
         animator.addListener(
            new AnimatorListenerAdapter() {
               // called when the animation finishes
               @Override
               public void onAnimationEnd(Animator animation) {
                  loadNextAnimal();
               }
            }
         );
      }
      else { // if the quizLinearLayout should animate in
         animator = ViewAnimationUtils.createCircularReveal(
            quizLinearLayout, centerX, centerY, 0, radius);
      }

      animator.setDuration(500); // set animation duration to 500 ms
      animator.start(); // start the animation
   }

   // called when a guess Button is touched
   private OnClickListener guessButtonListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
         Button guessButton = ((Button) v);
         String guess = guessButton.getText().toString();
         String answer = getCountryName(correctAnswer);
         ++totalGuesses; // increment number of guesses the user has made

         if (guess.equals(answer)) { // if the guess is correct
            ++correctAnswers; // increment the number of correct answers

            // display correct answer in green text
            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(
               getResources().getColor(R.color.correct_answer,
                  getContext().getTheme()));

            disableButtons(); // disable all guess Buttons

            // if the user has correctly identified ANIMALS_IN_QUIZ animals
            if (correctAnswers == ANIMALS_IN_QUIZ) {
               // DialogFragment to display quiz stats and start new quiz
               DialogFragment quizResults =
                  new DialogFragment() {
                     // create an AlertDialog and return it
                     @Override
                     public Dialog onCreateDialog(Bundle bundle) {
                        AlertDialog.Builder builder =
                           new AlertDialog.Builder(getActivity());
                        builder.setMessage(
                           getString(R.string.results,
                              totalGuesses,
                              (1000 / (double) totalGuesses)));

                        // "Reset Quiz" Button
                        builder.setPositiveButton(R.string.reset_quiz,
                           new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog,
                                 int id) {
                                 resetQuiz();
                              }
                           }
                        );

                        return builder.create(); // return the AlertDialog
                     }
                  };

               // use FragmentManager to display the DialogFragment
               quizResults.setCancelable(false);
               quizResults.show(getFragmentManager(), "quiz results");
            }
            else { // answer is correct but quiz is not over
               // load the next animal after a 2-second delay
               handler.postDelayed(
                  new Runnable() {
                     @Override
                     public void run() {
                        animate(true); // animate the animal off the screen
                     }
                  }, 2000); // 2000 milliseconds for 2-second delay
            }
         }
         else { // answer was wrong
//
//            // display "WRONG!" in red
//            answerTextView.setText(R.string.incorrect_answer);
//            answerTextView.setTextColor(getResources().getColor(
//               R.color.incorrect_answer, getContext().getTheme()));
            MainActivity main = new MainActivity();
            Toast.makeText(getActivity(),
                    R.string.incorrect_answer,
                    Toast.LENGTH_SHORT).show();
            guessButton.setEnabled(false); // disable incorrect answer
//            main.play(R.raw.horsewhinnying, soundOptions);
         }
      }
   };

   // utility method that disables all answer Buttons
   private void disableButtons() {
      for (int row = 0; row < guessRows; row++) {
         LinearLayout guessRow = guessLinearLayouts[row];
         for (int i = 0; i < guessRow.getChildCount(); i++)
            guessRow.getChildAt(i).setEnabled(false);
      }
   }
}
