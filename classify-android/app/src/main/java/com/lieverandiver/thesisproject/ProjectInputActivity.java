package com.lieverandiver.thesisproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.lieverandiver.thesisproject.adapter.ExamInputAdapter;
import com.lieverandiver.thesisproject.adapter.ProjectInputAdapter;
import com.remswork.project.alice.exception.GradingFactorException;
import com.remswork.project.alice.model.Exam;
import com.remswork.project.alice.model.Grade;
import com.remswork.project.alice.model.Project;
import com.remswork.project.alice.model.Student;
import com.remswork.project.alice.service.ClassService;
import com.remswork.project.alice.service.ExamService;
import com.remswork.project.alice.service.GradeService;
import com.remswork.project.alice.service.ProjectService;
import com.remswork.project.alice.service.impl.ClassServiceImpl;
import com.remswork.project.alice.service.impl.ExamServiceImpl;
import com.remswork.project.alice.service.impl.GradeServiceImpl;
import com.remswork.project.alice.service.impl.ProjectServiceImpl;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static com.lieverandiver.thesisproject.R.id.input_back1;
import static com.lieverandiver.thesisproject.R.id.input_back5;
import static com.lieverandiver.thesisproject.R.id.input_ok1;
import static com.lieverandiver.thesisproject.R.id.input_ok5;
import static com.lieverandiver.thesisproject.R.id.input_tryagain1;
import static com.lieverandiver.thesisproject.R.id.input_tryagain5;
import static com.lieverandiver.thesisproject.R.id.input_tryagainemp1;
import static com.lieverandiver.thesisproject.R.id.input_tryagainemp5;

/**
 * Created by Verlie on 8/31/2017.
 */

public class ProjectInputActivity extends AppCompatActivity implements View.OnClickListener {

    private final ClassService classService = new ClassServiceImpl();
    private final ProjectService projectService = new ProjectServiceImpl();
    private final GradeService gradeService = new GradeServiceImpl();


    List<Student> studentList = new ArrayList<>();
    private EditText editTextName;
    private TextView textViewDate;
    private ToggleButton buttonSubmit;
    private RecyclerView recyclerViewStudentInput;
    private EditText editTextTotal;
    private Button btnBack;
    private CardView dialogSucces;
    private CardView dialogFailed;
    private Button btnTryAgain;
    private Button btnOk;
    private ProjectInputAdapter studentAdapter;
    private CardView getDialogEmptyTotal;
    private Button getBtnTryAgainEmptyTotal;
    private ToggleButton toggleButtonhideandshow;
    private FrameLayout frameLayouthideandshow;
    private Grade grade;
    ;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_z_input_project);
        init();

        buttonSubmit.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                final long classId = getIntent().getExtras().getLong("classId");

                try {
                    Project project = new Project();
                    project.setTitle(!editTextName.getText().toString().trim().isEmpty() ?
                            editTextName.getText().toString().trim() : "Project");
                    project.setDate(textViewDate.getText().toString());

                    if (editTextTotal.getText().toString().equals("")) {
                        toggleButtonhideandshow.setChecked(false);
                        getDialogEmptyTotal.setVisibility(View.VISIBLE);
                        recyclerViewStudentInput.setVisibility(View.GONE);
                        return;
                    } else {
                        project.setItemTotal(Integer.parseInt(editTextTotal.getText().toString()));
                    }

                    studentAdapter.setTotalItem(project.getItemTotal());
                    studentAdapter.onValidate(true);

                    if (studentAdapter.isNoError()) {
                        project = projectService.addProject(project, classId, 1L);
                        for (int i = 0; i < studentList.size(); i++) {
                            //Student id
                            final long studentId = studentList.get(i).getId();
                            //
                            int score = studentAdapter.getScore(i);
                            Student student = studentList.get(i);
                            projectService.addProjectResult(score, project.getId(), student.getId());

                            //Adding Grade for project
                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    try {
                                        final List<Project> projectList = projectService.getProjectListByClassId(classId);
                                        final double fProject[] = new double[projectList.size()];
                                        final long sId = studentId;
                                        double tempTotal = 0;

                                        try {
                                            List<Grade> tempList = gradeService.getGradeListByClass(classId, sId, 1L);
                                            grade = (tempList.size() > 0 ? tempList.get(0) : null);
                                        } catch (GradingFactorException e) {
                                            e.printStackTrace();
                                            grade = null;
                                        }
                                        if (grade == null) {
                                            Grade _grade = new Grade();
                                            grade = gradeService.addGrade(_grade, classId, studentId, 1L);
                                        }

                                        final Grade lGrade = grade;
                                        final long gradeId = grade.getId();

                                        for (int i = 0; i < fProject.length; i++) {
                                            final double total = projectList.get(i).getItemTotal();
                                            final double score = projectService
                                                    .getProjectResultByProjectAndStudentId(
                                                            projectList.get(i).getId(), sId).getScore();
                                            fProject[i] = (score / total) * 100;
                                        }
                                        for (int i = 0; i < fProject.length; i++)
                                            tempTotal += fProject[i];

                                        //after looping
                                        if(fProject.length > 0)
                                            tempTotal /= fProject.length;
                                        else
                                            tempTotal = 0;

                                        lGrade.setActivityScore(tempTotal);
                                        lGrade.setTotalScore(lGrade.getTotalScore() + tempTotal);
                                        gradeService.updateGradeById(gradeId, lGrade);
                                    } catch (GradingFactorException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }
                        dialogSucces.setVisibility(View.VISIBLE);
                        Toast.makeText(ProjectInputActivity.this, "Success", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ProjectInputActivity.this, "Failed", Toast.LENGTH_LONG).show();
                        dialogFailed.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case input_back5:
                Intent intent = getIntent().setClass(this, ProjectAddActivity.class);
                startActivity(intent);
                this.finish();
                break;
            case input_ok5:
               intent = getIntent().setClass(this, ProjectAddActivity.class);
                startActivity(intent);
                this.finish();
                break;

            case input_tryagainemp5:

                buttonSubmit.setChecked(false);
                buttonSubmit.setVisibility(View.VISIBLE);
                getDialogEmptyTotal.setVisibility(View.GONE);
                recyclerViewStudentInput.setVisibility(View.VISIBLE);
                break;

            case input_tryagain5:
                buttonSubmit.setChecked(false);
                buttonSubmit.setVisibility(View.VISIBLE);
                dialogFailed.setVisibility(View.GONE);
                break;

        }
    }

    public void init(){

        try {
            editTextName = (EditText) findViewById(R.id.input_name5);
            editTextTotal =(EditText) findViewById(R.id.input_total5);
            textViewDate = (TextView) findViewById(R.id.input_date5);
            buttonSubmit = (ToggleButton) findViewById(R.id.input_submit5);
            btnBack = (Button) findViewById(input_back5);
            dialogFailed = (CardView)findViewById(R.id.input_failed5);
            dialogSucces = (CardView)findViewById(R.id.input_succes5);
            btnOk = (Button) findViewById(input_ok5);
            btnTryAgain = (Button) findViewById(input_tryagain5);
            getDialogEmptyTotal = (CardView) findViewById(R.id.input_failedemp5);
            getBtnTryAgainEmptyTotal =(Button) findViewById(input_tryagainemp5);

            toggleButtonhideandshow = (ToggleButton) findViewById(R.id.input_hideandshow5);
            frameLayouthideandshow = (FrameLayout)findViewById(R.id.input_detailts5);

            toggleButtonhideandshow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        frameLayouthideandshow.setVisibility(View.GONE);
                    }else{
                        frameLayouthideandshow.setVisibility(View.VISIBLE);
                    }
                }
            });

            getDialogEmptyTotal.setVisibility(View.GONE);
            dialogSucces.setVisibility(View.GONE);
            dialogFailed.setVisibility(View.GONE);

            btnOk.setOnClickListener(this);
            btnBack.setOnClickListener(this);

            buttonSubmit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        buttonSubmit.setVisibility(View.GONE);

                    }
                }
            });

            getBtnTryAgainEmptyTotal.setOnClickListener(this);
            btnTryAgain.setOnClickListener(this);

            recyclerViewStudentInput = (RecyclerView) findViewById(R.id.input_recyclerview5);
            recyclerViewStudentInput.setVisibility(View.VISIBLE);
            for(Student s : classService.getStudentList(getIntent().getExtras().getLong("classId")))
                studentList.add(s);

            studentAdapter = new ProjectInputAdapter(this, studentList);
            recyclerViewStudentInput.setAdapter(studentAdapter);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            recyclerViewStudentInput.setLayoutManager(layoutManager);
            recyclerViewStudentInput.setItemAnimator(new DefaultItemAnimator());

            String date = String.format(Locale.ENGLISH, "%02d/%02d/%d" , Calendar.getInstance().get(Calendar.MONTH),
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + 1,
                    Calendar.getInstance().get(Calendar.YEAR));
            textViewDate.setText(date);


        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    public interface InputListener {
        void onValidate(boolean doValidate);
        boolean isNoError();
        int getScore(int index);
        void setTotalItem(int score);
    }

}
