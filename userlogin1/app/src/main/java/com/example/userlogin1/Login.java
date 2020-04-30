package com.example.userlogin1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {
    EditText lEmail,lPassword;
    Button lLogin;
    TextView lRegister;
    FirebaseAuth fAuth;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //intializing instances
        lEmail = findViewById(R.id.loginEmail);
        lPassword = findViewById(R.id.loginPassword);
        lLogin = findViewById(R.id.login);
        lRegister = findViewById(R.id.textRegister);
        progressBar = findViewById(R.id.loginprogressBar);

        fAuth = FirebaseAuth.getInstance();

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
                }); //user created if successful
            }
        });

    }
}
