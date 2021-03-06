package com.gatech.edu.soloTechno.m4_login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.R.attr.name;


/**
 * Created by timothybaba on 2/19/17.
 */

public class RegisterActivity extends AppCompatActivity {

    /* ************************
        Widgets needed for binding and getting information
     */
    private Spinner accountTypeSpinner;
    private EditText firstName_text;
    private EditText lastName_text;
    private EditText email_text;
    private EditText password_text;
    private EditText confirmPassword_text;

    /**
     * variables to hold data retrieved from widgets
     */
    private String accountType;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String confirmPassword;
    private boolean validEmail;
    private boolean validPassword;
    private boolean validFirstName;
    private boolean validLastName;
    private boolean edit = false;

    // firebase
    private DatabaseReference mDatabase;
    private String mUserId;
    private FirebaseUser mFirebaseUser;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth mAuth;
    public static final String TAG = RegisterActivity.class.getSimpleName();
    private ProgressDialog mAuthProgressDialog;
    private DatabaseReference myRootRef;



    public static List<String> accounts = Arrays.asList("Manager", "Worker", "Admin", "User");


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_register);



        /**
         * Grab the dialog widgets so we can get info for later
         */
        accountTypeSpinner = (Spinner) findViewById(R.id.spinner4);
        firstName_text = (EditText) findViewById(R.id.first_Name);
        lastName_text = (EditText) findViewById(R.id.last_Name);
        email_text = (EditText) findViewById(R.id.email);
        password_text = (EditText) findViewById(R.id.password);
        confirmPassword_text = (EditText) findViewById(R.id.confirm_Password);

        /**
         * Creates an object that accesses the tools provided in the Firebase Authentication SDK
         */
        mAuth = FirebaseAuth.getInstance();
        myRootRef = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() != null) {
            ValueEventListener postListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    lastName_text.setText((CharSequence) dataSnapshot.child("Last Name").getValue());
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting Post failed, log a message
                    Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                    // ...
                }
            };
            myRootRef.addValueEventListener(postListener);
        }

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mFirebaseUser = firebaseAuth.getCurrentUser();
                if (mFirebaseUser != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + mFirebaseUser.getUid());
                    email_text.setText(mFirebaseUser.getEmail());
                    firstName_text.setText(mFirebaseUser.getDisplayName());

                    // We move to to the Main Activity only when we validate the user has been
                    // created iin firebase
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                    // We only populate the database with the user's info when we validate the user
                    // has been successfully created in firebase
                    createFirebaseUserProfile(mFirebaseUser);

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // [START_EXCLUDE]
                // [END_EXCLUDE]
            }
        };


        /**
         * Creates a save button and defines an on-click listener than calls the submitForm method
         * once the button is pressed
         */

        final Button saveButton = (Button) findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                submitForm();
            }
        });

        /**
         * set up adapter to display the account types in the spinner
         */
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, accounts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountTypeSpinner.setAdapter(adapter);

    }

    /**
     * Displays an animated progress indicator along with a customized message to inform a user when
     * the app is in process of authenticating his or her account
     */
    private void createAuthProgressDialog() {
        mAuthProgressDialog = new ProgressDialog(this);
        mAuthProgressDialog.setTitle("Loading...");
        mAuthProgressDialog.setMessage("Authenticating with Firebase...");
        mAuthProgressDialog.setCancelable(false);
    }

    /**
     * Private helper method to register the user through Firebase authentication. After user
     * submits information, method reads in email and password, calls the Firebase instance to
     * add new users to the system.
     */

    private void submitForm() {

        accountType = accountTypeSpinner.getSelectedItem().toString().trim();
        email = email_text.getText().toString().trim();
        password = password_text.getText().toString().trim();
        firstName = firstName_text.getText().toString().trim();
        lastName = lastName_text.getText().toString().trim();
        confirmPassword = confirmPassword_text.getText().toString().trim();
        validEmail = isValidEmail(email);
        validFirstName = isValidName(firstName);
        validLastName = isValidName(lastName);
        validPassword = isValidPassword(password, confirmPassword);

        if (!validEmail || !validFirstName || !validLastName || !validPassword) return;

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
            return;
        }



        createAuthProgressDialog();
        mAuthProgressDialog.show();


        if(mAuth.getCurrentUser() != null){
            mAuth.getCurrentUser().updatePassword(confirmPassword);
        } else {
            //create user
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            mAuthProgressDialog.dismiss();
                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            if (!task.isSuccessful()) {
                                Toast.makeText(RegisterActivity.this, "Authentication failed." + task.getException(),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d(TAG, "Authentication successful");
                                createFirebaseUserProfile(task.getResult().getUser());
                            }
                        }
                    });
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Log in failed." + task.getException(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
<<<<<<< HEAD
                            Log.d(TAG, "Authentication successful");
=======
                            // Sign user in with email
                            Log.d(TAG, "Log in successful");
                            //    Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
                            //   startActivity(mainActivity);
>>>>>>> d9a8e81a072528dc447973102a344f1435d859f8
                        }
                    }
                });
    }

    /**
     * Adds the authentication state listener to the Firebase Authentication object
     */
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    /**
     * removes the authentication state listener before the RegisterActivity is destroyed
     */
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    /**
     * Uses an Android pattern to check if an entered email is in the correct format. If the email
     * is not valid, it displays an error in the email_text
     * @param email email entered by a user
     * @return whether an email is valid or not
     */
    private boolean isValidEmail(String email) {
        boolean isGoodEmail =
                (email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches());
        if (!isGoodEmail) {
            email_text.setError("Please enter a valid email address");
            return false;
        }
        return isGoodEmail;
    }

    /**
     * Ensures the name field has not been left blank. It it has, it displays an error in either
     * the firstName_text or the lastName_text depending on which has been left blank
     * @param name name entered by a user
     * @return whether a name field is blank or not
     */
    private boolean isValidName(String name) {
        if (name.equals("")) {
            if (name.equals(firstName_text.getText().toString().trim())) {
                firstName_text.setError("Please enter your name");
            } else {
                lastName_text.setError("Please enter your name");
            }

            return false;
        }
        return true;
    }

    /**
     * Confirms that an entered password is atleast 6 characters long, and ensures the password and
     * the password confirmation fields match. if not the case, it displays an error  int the
     * password_text
     * @param password password entered in the password field
     * @param confirmPassword password entered in the confirm password field
     * @return whether password is 6 characters long and both the password and confirmPassword fields
     * match
     */

    private boolean isValidPassword(String password, String confirmPassword) {
        if (password.length() < 6) {
            password_text.setError("Please create a password containing at least 6 characters");
            return false;
        } else if (!password.equals(confirmPassword)) {
            password_text.setError("Passwords do not match");
            return false;
        }
        return true;
    }

    /**
     * Sets and attaches the name of a user to his or her user profile. The app uses this name to
     * greet the user once he or she successfully logs in.
     * @param user current user that was just recently created
     */
    private void createFirebaseUserProfile(final FirebaseUser user) {

        UserProfileChangeRequest addProfileName = new UserProfileChangeRequest.Builder()
                .setDisplayName(firstName)
                .build();

        myRootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference firstNameRef =  myRootRef.child("First Name");
        firstNameRef.setValue(firstName);
        DatabaseReference lastNameRef =  myRootRef.child("Last Name");
        lastNameRef.setValue(lastName);
        DatabaseReference accountTypeRef =  myRootRef.child("Account Type");
        accountTypeRef.setValue(accountType);

        user.updateProfile(addProfileName)
                .addOnCompleteListener(new OnCompleteListener<Void>() {

                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, user.getDisplayName());
                        }
                    }

                });
    }
}

