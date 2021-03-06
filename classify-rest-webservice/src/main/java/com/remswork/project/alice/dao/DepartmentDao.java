package com.remswork.project.alice.dao;

import com.remswork.project.alice.exception.DepartmentException;
import com.remswork.project.alice.model.Department;

import java.util.List;

public interface DepartmentDao {

    Department getDepartmentById(long id) throws DepartmentException;

    List<Department> getDepartmentList() throws DepartmentException;

    Department addDepartment(Department department) throws DepartmentException;

    Department updateDepartmentById(long id, Department newDepartment) throws DepartmentException;

    Department deleteDepartmentById(long id) throws DepartmentException;

}
