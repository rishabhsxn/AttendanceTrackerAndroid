package com.example.userlogin1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
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


public class Login extends AppCompatActivity {
    EditText lEmail,lPassword;
    Button lLogin,GsignIn;
    TextView lRegister,forgotPassword;
    FirebaseAuth fAuth;
    ProgressBar progressBar;

    private GoogleSignInClient mGoogleSignInClient;
    private final static int RC_SIGN_IN = 123;

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
        setContentView(R.layout.activity_login);
        //intializing instances
        lEmail = findViewById(R.id.loginEmail);
        lPassword = findViewById(R.id.loginPassword);
        lLogin = findViewById(R.id.login);
        GsignIn = findViewById(R.id.google_signIn);
        lRegister = findViewById(R.id.textRegister);
        forgotPassword = findViewById(R.id.forgotPassword);
        progressBar = findViewById(R.id.loginprogressBar);

        fAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In by calling create request method
        createRequest();
        //check if someone clicks get signin method
        GsignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        lRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.INVISIBLE);
                startActivity(new Intent(getApplicationContext(),Register.class));
            }
        });

        lLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = lEmail.getText().toString().trim();
                String password = lPassword.getText().toString().trim();

                //first need to check if user has entered null value
                if(TextUtils.isEmpty(email)){
                    lEmail.setError("Email is required");
                    return;
                }
                if(TextUtils.isEmpty(password)){
                    lPassword.setError("Password is required");
                    return;
                }


                //Check password length more than 6
                if(password.length()<6){
                    lPassword.setError("more than 6 characters required!");
                    return;
                }

                //now All CLear, set the Progress Bar to spin
                progressBar.setVisibility(View.VISIBLE);

                //Authenticate the user
                fAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        progressBar.setVisibility(View.INVISIBLE);
                        if(task.isSuccessful()){
                                Toast.makeText(Login.this,"Login Successful!",Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        }else{
                            Toast.makeText(Login.this,"Error! "+ task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    }
                }); //user logged if successful
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText resetMail = new EditText(v.getContext());

                AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(v.getContext());

                passwordResetDialog.setTitle("Reset Password?");
                passwordResetDialog.setMessage("Enter your Email to receive Reset-Link ");
                passwordResetDialog.setView(resetMail);

                passwordResetDialog.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Extract the email and send Reset LInk
                        String mail = resetMail.getText().toString();
                        if(mail.isEmpty()){
                            resetMail.setError("Required!");
                            return;
                        }
                        fAuth.sendPasswordResetEmail(mail).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(Login.this,"Reset-link sent to your Email",Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(Login.this,"Unable to send Reset-link "+ e.getMessage(),Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                passwordResetDialog.setNegativeButton("Leave", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //redirect to login view
                    }
                });
                passwordResetDialog.create().show();
            }
        });

    }

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
    private void signIn() {

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
                Log.d("GooglesignIn: ", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("Error:", "Google sign in failed", e);
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
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("Google SignIn:", "signInWithCredential:success");
                            final FirebaseUser user = fAuth.getCurrentUser();
                            boolean newuser = task.getResult().getAdditionalUserInfo().isNewUser();
                            if(newuser){
//                                Log.d("New User","Go to registeration page for sign up!!!!");
                                Log.d("New User","Is being created!!!!");
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
                                            Toast.makeText(Login.this, "Verification Email has been sent!", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(Login.this, "Registered Successfully!", Toast.LENGTH_SHORT).show();

                            }else {
                                Log.d("User with","Google sign in::;:Already is a user");
                            }

                            startActivity(new Intent(getApplicationContext(),MainActivity.class));


                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Google SignIn", "signInWithCredential:failure", task.getException());
                            Toast.makeText(Login.this,"Error in authentication with Firebase!",Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }



}
