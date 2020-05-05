package com.example.userlogin1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
EditText rName,rEmail,rPassword,rPassword2;
Button rSignUp,Gsignup;
TextView rLogin;
FirebaseAuth fAuth;
ProgressBar progressBar;
FirebaseFirestore fStore;
String userId;

    private GoogleSignInClient mGoogleSignInClient;
    private final static int RC_SIGN_IN = 124;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if(currentUser!=null){
            startActivity(new Intent(getApplicationContext(),MainActivity.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //intializing instances
        rName = findViewById(R.id.registerName);
        rEmail = findViewById(R.id.registerEmail);
        rPassword = findViewById(R.id.registerPassword);
        rPassword2 = findViewById(R.id.registerPassword2);
        rSignUp = findViewById(R.id.SignUp);
        Gsignup = findViewById(R.id.google_signUp);
        rLogin = findViewById(R.id.textLogin);
        progressBar = findViewById(R.id.registerprogressBar);
        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In by calling create request method
        createRequest();
        //check if someone clicks get signin method
        Gsignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });

        //when the user clicks register button need to validate the email, < 6 character password. and create user
        rSignUp.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                final String email = rEmail.getText().toString().trim();
                String password = rPassword.getText().toString().trim();
                String password2 = rPassword2.getText().toString().trim();
                final String name = rName.getText().toString();
                //first need to check if user has entered null value
                if(TextUtils.isEmpty(email)){
                    rEmail.setError("Email is required");
                    return;
                }
                if(TextUtils.isEmpty(name)){
                    rEmail.setError("Enter your Name");
                    return;
                }
                if(TextUtils.isEmpty(password)){
                    rPassword.setError("Password is required");
                    return;
                }
                if(TextUtils.isEmpty(password2)){
                    rPassword2.setError("Confirm the password!");
                    return;
                }

                //Check password length more than 6
                if(password.length()<6){
                    rPassword.setError("more than 6 characters required!");
                    return;
                }

                if(!password.equals(password2)){
                    rPassword2.setError("Incorrect!");
                    return;
                }

                //now All CLear, set the Progress Bar to spin
                progressBar.setVisibility(View.VISIBLE);

                //create user
                fAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){

                            //need to verify the email:
                            FirebaseUser Vuser = fAuth.getCurrentUser();
                            Vuser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(Register.this,"Verification Email has been sent!",Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("Verification Email:","Failure to send because: "+ e.getMessage());
                                }
                            });


                            //adding new user details to users collection in firestore ->Collection = useres -> document identifier = userId
                            userId = fAuth.getCurrentUser().getUid();
                            DocumentReference documentReference = fStore.collection("users").document(userId);

                            Map<String,Object> user = new HashMap<>(); //key is string value is object
                            user.put("fullName",name);
                            user.put("email",email);

                            //finally adding the user details<Information> via documentReference to userId Document
                            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("DocumentReference","OnSuccess: User profile created "+ userId);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("DocumentReference","Failure: "+e.toString());
                                }
                            });

                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(Register.this,"Registered Successfully!",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        }else{
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(Register.this,"Error! "+ task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    }
                }); //user created if successful
            }//onclick End
        });//buttonOnClick listner ends

        //if user wants to login
        rLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),Login.class));
            }
        });

    }//OnCreate Ends

    private void createRequest() {
            //it is creating or initializing a request that ask for user to sign in with one gmail out of all. But it is nor send to google right now
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            //created request using signinoption transfer google these request using signinClient
            // Build a GoogleSignInClient with the options specified by gso.
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

    }

    //request is created when user is on the activity but request is only send to google when the button is clicked
    private void signUp() {
        //intent is created to show that pop up to user for selecting an email
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        //rc_sign_in just an integer to signify the identifier of intent created
        startActivityForResult(signInIntent, RC_SIGN_IN);
        //now!!!! when user clicks or selects the email to sign in then that intent gives a result which is then catch by onActivityResult mentioned below:
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("GooglesignUP: ", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("Error:", "Google sign UP failed", e);
                Toast.makeText(this,"Error! " + e.getMessage(),Toast.LENGTH_SHORT).show();
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        fAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    //if on complete is successfull then user sign in successfully checked with firebase and can be logged in
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign up success, update UI with the signed-in user's information
                            Log.d("Google SignUp:", "signUPWithCredential:success");
                            final FirebaseUser user = fAuth.getCurrentUser();
                            //if user is signin for the first time we need to store his name and gmail additionally on firestore for that we need to know if he is new to sign in
                            boolean newuser = task.getResult().getAdditionalUserInfo().isNewUser();
                            if(newuser){

                                Log.d("New User","Is being created inside register page!!!!");
                                //Do Stuffs for new user
                                final String email = user.getEmail();
                                final String name = user.getDisplayName();

                                //first need to check if user has entered null value
                                if(TextUtils.isEmpty(email)){
                                    Log.d("Newuser!!: ","Email went empty idk y");
                                    return;
                                }
                                if(TextUtils.isEmpty(name)){
                                    Log.d("Newuser!!: ","Name went empty idk y");
                                    return;
                                }

                                //now All CLear, set the Progress Bar to spin
                                progressBar.setVisibility(View.VISIBLE);
                                //need to verify the email:
                                user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //Usually Google account logins are verified proactively so it will go to else part
                                        if(!user.isEmailVerified()) {
                                            Toast.makeText(Register.this, "Verification Email has been sent!", Toast.LENGTH_SHORT).show();
                                        }
                                        else{
                                            Log.d("User Email","s already verified");
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d("Verification Email:","Failure to send because: "+ e.getMessage());
                                    }
                                });


                                //adding new user details to users collection in firestore ->Collection = useres -> document identifier = userId
                                final String userId = fAuth.getCurrentUser().getUid();
                                FirebaseFirestore fStore = FirebaseFirestore.getInstance();;
                                DocumentReference documentReference = fStore.collection("users").document(userId);

                                Map<String,Object> Muser = new HashMap<>(); //key is string value is object
                                Muser.put("fullName",name);
                                Muser.put("email",email);

                                //finally adding the user details<Information> via documentReference to userId Document
                                documentReference.set(Muser).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("DocumentReference","OnSuccess: User profile created "+ userId);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d("DocumentReference","Failure: "+e.toString());
                                    }
                                });

                                progressBar.setVisibility(View.INVISIBLE);
                                Toast.makeText(Register.this, "Registered Successfully!", Toast.LENGTH_SHORT).show();

                            }else {
                                Log.d("User with","Google sign in::;:Already is a user");
                            }

                            startActivity(new Intent(getApplicationContext(),MainActivity.class));


                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Google Signup", "signInWithCredential:failure", task.getException());
                            Toast.makeText(Register.this,"Error in authentication with Firebase!",Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }


}//Activity Ends

//todo: Need to implement same function in register and login activity so instead of same code pasted write a global function for two activity
