package com.hfad.ad2noteapp.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hfad.ad2noteapp.App;
import com.hfad.ad2noteapp.OnItemClickListener;
import com.hfad.ad2noteapp.Prefs;
import com.hfad.ad2noteapp.R;
import com.hfad.ad2noteapp.models.Note;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private Prefs prefs;
    private ArrayList<Note> list;
    private boolean isUpdating;

    boolean click = true;
    boolean clickD = true;
    private int temp;
    private Bundle bundle;
    private Note note;
    private Button delete;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_home,
                container, false);
    }

    // this method runs once only when you launch the app
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        adapter = new NoteAdapter(getActivity());
        loadData();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        initList();

        view.findViewById(R.id.fab).setOnClickListener(v ->{
            isUpdating = false;
            openForm(null);
    });
        setFragmentListener();
    }

    private void loadData() {
        list = (ArrayList<Note>) App.getAppDataBase().noteDao().getAll();
        adapter.setList(list);
        Log.e("ertert", "loadData: " + " we have list setting in here one more time");
    }

    private void initList() {
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onClick(int position) {

                // here you get the note that you have clicked on the list

                note = adapter.getItem(position);
                temp = position;
                isUpdating = true;
                openForm(note);
            }

            @Override
            public void longClick(int position) { // StackOverFlow: Closing a custom alert dialog on button click

                LayoutInflater inflater = LayoutInflater.from(getContext());
                View view = inflater.inflate(R.layout.dialog_layout, null);

                delete = view.findViewById(R.id.delete);
                Button cancel = view.findViewById(R.id.cancel);

                AlertDialog.Builder alert = new AlertDialog.Builder(getContext())
                        .setView(view);

                final AlertDialog dialog = alert.create();

                delete.setOnClickListener(v -> {
                    App.getAppDataBase().noteDao().delete(adapter.getItem(position));
                    Log.e("ololo", "longClick: "+ adapter.getItem(position).getTitle());

                    Note note1 = adapter.getItem(position);

                    adapter.remove(position);

                    deleteFromFireStore(note1);

                    dialog.dismiss();
                });

                cancel.setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            }
        });
    }

    private void deleteFromFireStore(Note note) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference noteRef = db.collection("notes")
                .document(note.getNoteId());

        noteRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {

                    Log.e("GGG", "onComplete: Deleted " + note.getTitle());

                } else {

                    Log.e("GGG", "onComplete:  failed to delete");

                }
            }
        });
    }

    private void openForm(Note note) {

        Bundle bundle = new Bundle();
        bundle.putSerializable("note", note);
        NavController navController = Navigation.findNavController(requireActivity(),
                R.id.nav_host_fragment);
        navController.navigate(R.id.formFragment, bundle);
    }

    //========================Here we receive our notes from formFragment================================
    private void setFragmentListener() {
        getParentFragmentManager().setFragmentResultListener("rk_form", getViewLifecycleOwner(),
                (requestKey, result) -> {

                    note = (Note) result.getSerializable("note");
                    if (isUpdating && !list.isEmpty()){

                        adapter.setItem(note, temp);

                    } else {

                        adapter.addItem(note);
                        Log.e("GGG", "setFragmentListener:  add is running " + note.getTitle() );
                    }
                });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu,
                                    @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        prefs = new Prefs(requireContext());

        switch (item.getItemId()) {

            case R.id.clear_settings:
                prefs.clearS();
                requireActivity().finish();
                return true;

            case R.id.test:
                Sort();
                return true;

            case R.id.sortDate:
                SortDate();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void SortDate() {
        list = (ArrayList<Note>) App.getAppDataBase().noteDao().getAll();
        if (clickD) {
            clickD = false;
            Collections.sort(list, new Comparator<Note>() {
                @Override
                public int compare(Note o1, Note o2) {
                    if (o1.getDate() == null || o2.getDate() == null)
                        return 0;
                    return o1.getDate().compareTo(o2.getDate());
                }
            });
        } else {
            Collections.sort(list, new Comparator<Note>() {
                @Override
                public int compare(Note o1, Note o2) {
                    if (o1.getDate() == null || o2.getDate() == null)
                        return 0;
                    return o2.getDate().compareTo(o1.getDate());
                }
            });

            clickD = true;
        }
        adapter.setList(list);
    }

    private void Sort() {
        list = (ArrayList<Note>) App.getAppDataBase().noteDao().getAll();

        if (click) {
            click = false;

            Collections.sort(list, new Comparator<Note>() {
                @Override
                public int compare(Note o1, Note o2) {
                    if (o1.getTitle() == null || o2.getTitle() == null)
                        return 0;
                    return o1.getTitle().compareTo(o2.getTitle());
                }
            });
        } else {
            Collections.reverse(list);
            click = true;
        }
        adapter.setList(list);
    }
}