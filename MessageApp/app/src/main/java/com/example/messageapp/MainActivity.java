package com.example.messageapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.example.messageapp.asyncTask.AsyncTaskRunner;
import com.example.messageapp.asyncTask.Callback;
import com.example.messageapp.database.model.ContactWithCredits;
import com.example.messageapp.database.service.ContactService;
import com.example.messageapp.database.service.CreditService;
import com.example.messageapp.network.HttpManager;
import com.example.messageapp.util.Bank;
import com.example.messageapp.database.model.Contact;
import com.example.messageapp.database.model.Credit;
import com.example.messageapp.util.JsonParser;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MainActivity extends AppCompatActivity {

    public static final String BANK_LIST = "Bank list";
    public static final int ADD_CONTACT_REQUEST_CODE = 200;
    private final String URL="https://jsonkeeper.com/b/F7QV";
    private BottomNavigationView bottomNavigationView;
    private List<Contact> contacts=new ArrayList<>();
    private List<Credit>credits=new ArrayList<>();
    private List<Bank>bankList=new ArrayList<>();
    private List<ContactWithCredits>contactWithCreditsList=new ArrayList<>();

    private AsyncTaskRunner asyncTaskRunner = new AsyncTaskRunner();

    private CardView btnContacte;
    private CardView btnAdaugaContact;
    private CardView btnMesaje;
    private CardView  btnRapoarte;

    private ContactService contactService;

    private List<Contact>contactsAux=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getDatasFromHttp();
        initComponents();
        contactService = new ContactService(getApplicationContext());
        //ca sa verific daca bd nu are date. Daca nu are, inserez de la json. Daca are, nu mai inserez
        contactService.getAllContacts(getAllContactsFromDbCallback());
    }

    private Callback<List<Contact>> getAllContactsFromDbCallback() {
        return new Callback<List<Contact>>() {
            @Override
            public void runResultOnUiThread(List<Contact> result) {
                if (result != null) {
                    contactsAux.addAll(result);
                }
            }
        };
    }

    private void initComponents() {

        btnContacte=findViewById(R.id.cv_contacte);
        btnAdaugaContact=findViewById(R.id.cv_adauga_contact);
        btnMesaje=findViewById(R.id.cv_mesaje);
        btnRapoarte=findViewById(R.id.cv_rapoarte);
        bottomNavigationView=findViewById(R.id.bottom_nav_view);
        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_view_acasa);
        bottomNavigationView.setOnNavigationItemSelectedListener(addBottomNavView());
        btnContacte.setOnClickListener(openContactsActivity());
        //ADAUGAM CONTACT => FACEM TRANSFER INTRE PARAMETRII
        btnAdaugaContact.setOnClickListener(AddContactClickEventListener());
        btnMesaje.setOnClickListener(openMessagesActivity());
        btnRapoarte.setOnClickListener(openRaportsActivity());
    }

    private View.OnClickListener openRaportsActivity() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(), RaportsActivity.class);
                startActivity(intent);
            }
        };
    }

    private View.OnClickListener openMessagesActivity() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(), MessagesActivity.class);
                startActivity(intent);
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_CONTACT_REQUEST_CODE
                && resultCode == RESULT_OK && data != null) {
            ContactWithCredits contactWithCredits = (ContactWithCredits) data
                    .getParcelableExtra(AddContactActivity.CONTACT_WITH_CREDITS_KEY);

            if (contactWithCredits != null) {
                if(contactWithCredits.credits==null){
                    contactService.insert(insertContactIntoDbCallback(), contactWithCredits.contact);
                    Log.i("Contact simplu",contactWithCredits.contact.toString());
                }else{
                    contactService.insert(insertContactWithCreditsIntoDbCallback(),contactWithCredits);
                    Log.i("Contact cu credite",contactWithCredits.toString());

                }
            }
        }
    }

    private View.OnClickListener AddContactClickEventListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(), AddContactActivity.class);
                startActivityForResult(intent, ADD_CONTACT_REQUEST_CODE);
            }
        };
    }

    private View.OnClickListener openContactsActivity() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(), ContactsActivity.class);
                startActivity(intent);
            }
        };
    }

    private BottomNavigationView.OnNavigationItemSelectedListener addBottomNavView() {
        return new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId()==R.id.bottom_nav_view_acasa){
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    overridePendingTransition(0,0);
                    return true;
                }
                if(item.getItemId()==R.id.bottom_nav_view_detalii_banca){
                    Intent intent=new Intent(getApplicationContext(), BankActivity.class);
                    intent.putExtra(BANK_LIST,(Serializable)bankList);
                    startActivity(intent);
                    overridePendingTransition(0,0);
                    return true;
                }
                if(item.getItemId()==R.id.bottom_nav_view_profil){
                    Intent intent=new Intent(getApplicationContext(),ProfileActivity.class);
                    intent.putExtra(BANK_LIST,(Serializable)bankList);
                    startActivity(intent);
                    overridePendingTransition(0,0);
                    return true;
                }
                return false;
            }
        };
    }
    private void getDatasFromHttp() {
        Callable<String> asyncOperation = new HttpManager(URL);
        Callback<String> mainThreadOperation = receiveDatasFromHttp();
        asyncTaskRunner.executeAsync(asyncOperation, mainThreadOperation);

    }
    private Callback<String> receiveDatasFromHttp() {
        return new Callback<String>() {
            @Override
            public void runResultOnUiThread(String result) {
                JsonParser.fromJson(result,contacts,bankList,credits);
                if(contactsAux.size()==0) {
                    int i=0;
                    for( Credit credit: credits){
                        List<Credit> lista=new ArrayList<>();
                        lista.add(credit);
                        ContactWithCredits contactWithCredits=new ContactWithCredits(contacts.get(i),lista);
                        i++;
                        contactService.insert(insertContactWithCreditsIntoDbCallback(),contactWithCredits);
                    }
                }
            }
        };
    }
    private Callback<Contact> insertContactIntoDbCallback() {
        return new Callback<Contact>() {
            @Override
            public void runResultOnUiThread(Contact result) {
                if (result != null) {
                    contactsAux.add(result);
                }
            }
        };
    }

    private Callback<ContactWithCredits> insertContactWithCreditsIntoDbCallback() {
        return new Callback<ContactWithCredits>() {
            @Override
            public void runResultOnUiThread(ContactWithCredits result) {
                if (result != null) {
                    contactWithCreditsList.add(result);
                }
            }
        };
    }
}