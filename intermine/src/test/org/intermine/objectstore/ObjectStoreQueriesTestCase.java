package org.flymine.objectstore;

import junit.framework.TestCase;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.*;
import java.beans.*;
import java.net.URL;
import java.io.*;

import org.exolab.castor.mapping.*;
import org.exolab.castor.xml.*;

import org.flymine.model.testmodel.*;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.query.*;
import org.flymine.sql.Database;
import org.flymine.util.ModelUtil;
import org.flymine.util.TypeUtil;

/**
 * TestCase for testing FlyMine Queries or FlyMine data
 * To check results:
 * add results to the results map
 * override executeTest to run query and assert that the result is what is expected
 */

public abstract class ObjectStoreQueriesTestCase extends QueryTestCase
{
    protected Map data = new LinkedHashMap();
    protected Map queries = new HashMap();
    protected Map results = new LinkedHashMap();

    // These must be given if the data is to be stored in a database
    protected ObjectStoreWriter writer;
    protected Database db;

    /**
     * Constructor
     */
    public ObjectStoreQueriesTestCase(String arg) {
        super(arg);
    }

    /**
     * Set up the test
     *
     * @throws Exception if an error occurs
     */
    public void setUp() throws Exception {
        super.setUp();
        setUpData();
        setUpQueries();
        setUpResults();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Set up all the results expected for a given subset of queries
     *
     * @throws Exception if an error occurs
     */
    public void setUpResults() throws Exception {
    }

    /**
     * Execute a test for a query. This should run the query and
     * contain an assert call to assert that the returned results are
     * those expected.
     *
     * @param type the type of query we are testing (ie. the key in the queries Map)
     * @throws Exception if type does not appear in the queries map
     */
    public abstract void executeTest(String type) throws Exception;

    /**
     * Test the queries produce the appropriate result
     *
     * @throws Exception if an error occurs
     */
    public void testQueries() throws Throwable {
        Iterator i = results.keySet().iterator();
        while (i.hasNext()) {
            String type = (String) i.next();
            // Does this appear in the queries map;
            if (!(queries.containsKey(type))) {
                throw new Exception(type + " does not appear in the queries map");
            }
            try {
                executeTest(type);
            } catch (Throwable t) {
                throw new Throwable("Failed on " + type, t);
            }
        }
    }


    /**
     * Set up any data needed
     *
     * @throws Exception if an error occurs
     */
    public void storeData() throws Exception {
        if ((writer == null) || (db == null)) {
            throw new NullPointerException("writer and db must be set before trying to store data");
        }
        long start = new Date().getTime();
        try {
            writer.beginTransaction();
            Iterator iter = data.keySet().iterator();
            while (iter.hasNext()) {
                writer.store(data.get(iter.next()));
            }
            writer.commitTransaction();
        } catch (Exception e) {
            writer.abortTransaction();
            throw new Exception(e);
        }

        java.sql.Connection con = db.getConnection();
        java.sql.Statement s = con.createStatement();
        con.setAutoCommit(true);
        s.execute("vacuum analyze");
        con.close();
        System.out.println("Took " + (new Date().getTime() - start) + " ms to set up data and VACUUM ANALYZE");
    }

    public void removeDataFromStore() throws Exception {
        if (writer == null) {
            throw new NullPointerException("writer must be set before trying to store data");
        }
        try {
            writer.beginTransaction();
            Iterator iter = data.keySet().iterator();
            while (iter.hasNext()) {
                writer.delete(data.get(iter.next()));
            }
            writer.commitTransaction();
        } catch (Exception e) {
            writer.abortTransaction();
            throw new Exception(e);
        }
    }

    /**
     * Set up the set of queries we are testing
     *
     * @throws Exception if an error occurs
     */
    public void setUpQueries() throws Exception {
        queries.put("SelectSimpleObject", selectSimpleObject());
        queries.put("SubQuery", subQuery());
        queries.put("WhereSimpleEquals", whereSimpleEquals());
        queries.put("WhereSimpleNotEquals", whereSimpleNotEquals());
        queries.put("WhereSimpleLike", whereSimpleLike());
        queries.put("WhereEqualsString", whereEqualString());
        queries.put("WhereAndSet", whereAndSet());
        queries.put("WhereOrSet", whereOrSet());
        queries.put("WhereNotSet", whereNotSet());
        queries.put("WhereSubQueryField", whereSubQueryField());
        queries.put("WhereSubQueryClass", whereSubQueryClass());
        queries.put("WhereNotSubQueryClass", whereNotSubQueryClass());
        queries.put("WhereNegSubQueryClass", whereNegSubQueryClass());
        queries.put("WhereClassClass", whereClassClass());
        queries.put("WhereNotClassClass", whereNotClassClass());
        queries.put("WhereNegClassClass", whereNegClassClass());
        queries.put("WhereClassObject", whereClassObject());
        queries.put("Contains11", contains11());
        queries.put("ContainsNot11", containsNot11());
        queries.put("ContainsNeg11", containsNeg11());
        queries.put("Contains1N", contains1N());
        queries.put("ContainsN1", containsN1());
        queries.put("ContainsMN", containsMN());
        queries.put("ContainsDuplicatesMN", containsDuplicatesMN());
        queries.put("SimpleGroupBy", simpleGroupBy());
        queries.put("MultiJoin", multiJoin());
        queries.put("SelectComplex", selectComplex());
        queries.put("SelectClassAndSubClasses", selectClassAndSubClasses());
        queries.put("SelectInterfaceAndSubClasses", selectInterfaceAndSubClasses());
        queries.put("SelectInterfaceAndSubClasses2", selectInterfaceAndSubClasses2());
        queries.put("SelectInterfaceAndSubClasses3", selectInterfaceAndSubClasses3());
        //queries.put("SelectClassFromSubQuery", selectClassFromSubQuery());
    }

    /*
      select company
      from Company
    */
    public Query selectSimpleObject() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        return q1;
    }

    /*
      select subquery.company.name, subquery.alias
      from (select company, 5 as alias from Company) as subquery
    */
    public Query subQuery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(5));
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        q1.addToSelect(v1);
        Query q2 = new Query();
        q2.addFrom(q1);
        QueryField f1 = new QueryField(q1, c1, "name");
        QueryField f2 = new QueryField(q1, v1);
        q2.addToSelect(f1);
        q2.addToSelect(f2);
        return q2;
    }

    /*
      select name
      from Company
      where vatNumber = 1234
    */
    public Query whereSimpleEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(1234));
        QueryField f1 = new QueryField(c1, "vatNumber");
        QueryField f2 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f2);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where vatNumber! = 1234
    */
    public Query whereSimpleNotEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(1234));
        QueryField f1 = new QueryField(c1, "vatNumber");
        QueryField f2 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.NOT_EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f2);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where name like "Company%"
    */
    public Query whereSimpleLike() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where name = "CompanyA"
    */
    public Query whereEqualString() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where name LIKE "Company%"
      and vatNumber > 2000
    */
    public Query whereAndSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select name
      from Company
      where name LIKE "CompanyA%"
      or vatNumber > 2000
    */
    public Query whereOrSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("CompanyA%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.OR);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select name
      from Company
      where not (name LIKE "Company%"
      and vatNumber > 2000)
    */
    public Query whereNotSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        cs1.setNegated(true);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select department
      from Department
      where department.name IN (select name from Department)
      order by Department.name
    */
    public Query whereSubQueryField() throws Exception {
        QueryClass c1 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        QueryClass c2 = new QueryClass(Department.class);
        QueryField f2 = new QueryField(c2, "name");
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, f2);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        q2.addToOrderBy(f2);
        return q2;
    }

    /*
      select company
      from Company
      where company IN (select company from Company where name = "CompanyA")
    */
    public Query whereSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1));
        QueryClass c2 = new QueryClass(Company.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, c2);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select company
      from Company
      where company NOT IN (select company from Company where name = "CompanyA")
    */
    public Query whereNotSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1));
        QueryClass c2 = new QueryClass(Company.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.DOES_NOT_CONTAIN, c2);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select company
      from Company
      where not company IN (select company from Company where name = "CompanyA")
    */
    public Query whereNegSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1));
        QueryClass c2 = new QueryClass(Company.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, c2);
        sqc1.setNegated(true);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select c1, c2
      from Company c1, Company c2
      where c1 = c2
    */
    public Query whereClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, qc2);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select c1, c2
      from Company c1, Company c2
      where c1 != c2
    */
    public Query whereNotClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.NOT_EQUALS, qc2);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select c1, c2
      from Company c1, Company c2
      where not (c1 = c2)
    */
    public Query whereNegClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, qc2);
        cc1.setNegated(true);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select company,
      from Company
      where c1 = <company object>
    */
    public Query whereClassObject() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        Object obj = data.get("CompanyA");
        //obj hasn't actually been stored, so set id manually
        TypeUtil.setFieldValue(obj, "id", new Integer(42));
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, obj);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select department, manager
      from Department, Manager
      where department.manager = manager
      and department.name = "DepartmentA1"
    */

      public Query contains11() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "manager");
        QueryValue v1 = new QueryValue("DepartmentA1");
        QueryField qf1 = new QueryField(qc1, "name");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        Constraint c1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
      }

    /*
      select department, manager
      from Department, Manager
      where department.manager != manager
      and department.name = "DepartmentA1"
    */

      public Query containsNot11() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "manager");
        QueryValue v1 = new QueryValue("DepartmentA1");
        QueryField qf1 = new QueryField(qc1, "name");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.DOES_NOT_CONTAIN, qc2);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        Constraint c1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
      }

    /*
      select department, manager
      from Department, Manager
      where (not department.manager = manager)
      and department.name = "DepartmentA1"
    */

      public Query containsNeg11() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "manager");
        QueryValue v1 = new QueryValue("DepartmentA1");
        QueryField qf1 = new QueryField(qc1, "name");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2);
        cc1.setNegated(true);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        Constraint c1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
      }

    /*
      select company, department
      from Company, Department
      where company.departments contains department
      and company.name = "CompanyA"
    */
      public Query contains1N() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField qf1 = new QueryField(qc1, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        Constraint c1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
      }

    /*
      select department, company
      from Department, company
      where department.company = company
      and company.name = "CompanyA"
    */
      public Query containsN1() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "company");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField qf1 = new QueryField(qc2, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        Constraint c1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
      }

    /*
      select contractor, company
      from Contractor, Company
      where contractor.companys contains company
      and contractor.name = "ContractorA"
    */
    public Query containsMN() throws Exception {
        QueryClass qc1 = new QueryClass(Contractor.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "companys");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("ContractorA");
        QueryField qf1 = new QueryField(qc1, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        Constraint c1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select contractor, company
      from Contractor, Company
      where contractor.oldComs CONTAINS company
    */
    public Query containsDuplicatesMN() throws Exception {
        QueryClass qc1 = new QueryClass(Contractor.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "oldComs");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(cc1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select company, count(*)
      from Company, Department
      where company contains department
      group by company
    */
    public Query simpleGroupBy() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS,  qc2);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(new QueryFunction());
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.setConstraint(cc1);
        q1.addToGroupBy(qc1);
        return q1;
    }

    /*
      select company, department, manager, address
      from Company, Department, Manager, Address
      where company contains department
      and department.manager = manager
      and manager.address = address
      and manager.name = "EmployeeA1"
    */
    public Query multiJoin() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryClass qc3 = new QueryClass(Manager.class);
        QueryClass qc4 = new QueryClass(Address.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        QueryReference qr2 = new QueryObjectReference(qc2, "manager");
        QueryReference qr3 = new QueryObjectReference(qc3, "address");
        QueryField qf1 = new QueryField(qc3, "name");
        QueryValue qv1 = new QueryValue("EmployeeA1");

        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qc3);
        q1.addToSelect(qc4);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        q1.addFrom(qc4);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2));
        cs1.addConstraint(new ContainsConstraint(qr2, ContainsConstraint.CONTAINS, qc3));
        cs1.addConstraint(new ContainsConstraint(qr3, ContainsConstraint.CONTAINS, qc4));
        cs1.addConstraint(new SimpleConstraint(qf1, SimpleConstraint.EQUALS, qv1));
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select avg(company.vatNumber) + 20, department.name, department
      from Company, Department
      group by department
    */
    public Query selectComplex() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryClass c2 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        QueryField f3 = new QueryField(c2, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addFrom(c2);
        QueryExpression e1 = new QueryExpression(new QueryFunction(f2, QueryFunction.AVERAGE),
                QueryExpression.ADD, new QueryValue(new Integer(20)));
        q1.addToSelect(e1);
        q1.addToSelect(f3);
        q1.addToSelect(c2);
        q1.addToGroupBy(c2);
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS AND ALL EMPLOYEES
      select employee
      from Employee
      order by employee.name
    */
    public Query selectClassAndSubClasses() throws Exception {
        QueryClass qc1 = new QueryClass(Employee.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        QueryField f1 = new QueryField(qc1, "name");
        q1.addToOrderBy(f1);
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS, CONTRACTORS AND ALL EMPLOYEES
      select employable
      from Employable
    */
    public Query selectInterfaceAndSubClasses() throws Exception {
        QueryClass qc1 = new QueryClass(Employable.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        return q1;
    }

    /*
      SHOULD PICK UP THE DEPARTMENTS AND COMPANIES
      select randominterface
      from RandomInterface
    */
    public Query selectInterfaceAndSubClasses2() throws Exception {
        QueryClass qc1 = new QueryClass(RandomInterface.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS AND CONTRACTORS
      select ImportantPerson
      from ImportantPerson
    */
    public Query selectInterfaceAndSubClasses3() throws Exception {
        QueryClass qc1 = new QueryClass(ImportantPerson.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        return q1;
    }

    /*
      select company
      from (select company from Company) as subquery, Company
      where company = subquery.company
    */
    /*
     * TODO: this currently cannot be done.
    public Query selectClassFromSubQuery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryClass c2 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        Query q2 = new Query();
        q2.addFrom(q1);
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(new ClassConstraint(c1, ClassConstraint.EQUALS, c2));
        return q2;
    }
    */

    private void setUpData() throws Exception {

        URL mapFile = getClass().getClassLoader().getResource("castor_xml_testmodel.xml");
        Mapping map = new Mapping();
        map.loadMapping(mapFile);

        URL testdataUrl = ObjectStoreQueriesTestCase.class.getClassLoader()
            .getResource("test/testmodel.xml");

        Reader reader = new FileReader(testdataUrl.getFile());
        Unmarshaller unmarshaller = new Unmarshaller(map);
        List result = (List)unmarshaller.unmarshal(reader);
        map(flatten(result));
    }

    private void map(Collection c) throws Exception {
        Iterator iter = c.iterator();
        while(iter.hasNext()) {
            Object o = iter.next();
            Method name = null;
            try {
                name = o.getClass().getMethod("getName", new Class[] {});
            } catch (Exception e) {}
            if(name!=null) {
                data.put((String)name.invoke(o, new Object[] {}), o);
            } else {
                data.put(new Integer(o.hashCode()), o);
            }
        }
    }

    private Collection flatten(Collection c) throws Exception {
        List toStore = new ArrayList();
        Iterator i = c.iterator();
        while(i.hasNext()) {
            flatten_(i.next(), toStore);
        }
        return toStore;
    }

    private void flatten_(Object o, Collection c) throws Exception {
        if(o == null || c.contains(o)) {
            return;
        }
        c.add(o);
        Method[] getters = TypeUtil.getGetters(o.getClass());
        for(int i=0;i<getters.length;i++) {
            Method getter = getters[i];
            Class returnType = getter.getReturnType();
            if(ModelUtil.isCollection(returnType)) {
                Iterator iter = ((Collection)getter.invoke(o, new Object[] {})).iterator();
                while(iter.hasNext()) {
                    flatten_(iter.next(), c);
                }
            } else if(ModelUtil.isReference(returnType)) {
                flatten_(getter.invoke(o, new Object[] {}), c);
            }
        }
    }

}
