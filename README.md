# Test Case Generator

This project is a simple library
to help generating test cases easier.

## How to Generate Test Cases

### Step 1
Create an ordinary Java class *with* default constructor:
```java
public class Sum { // default directory name would be name of class
    public Sum() {} // don't override default constructor
    
    @edu.sharif.cs.contests.Init
    public void initialization() { // no parameters
        // do some initialization stuff if necessary 
    }
    
    /**
    * 
    * @param pw write your values into this PrintWriter.
    *           It would automatically be written on files.
    */
    @edu.sharif.cs.contests.SampleInputs
    public void sample1(java.io.PrintWriter pw) {
        pw.println("2 2"); // use your custom sample input
    }
    
    @edu.sharif.cs.contests.SampleInputs
    public void sample2(java.io.PrintWriter pw) { // you can have as many sample inputs as you want
        pw.println("5 6");
    }
    
    @edu.sharif.cs.contests.InputTest
    public void generateInputs(java.io.PrintWriter pw, java.util.Random rnd) {
        // this function will be called each time to create a input file.
        // use provided random object to create random numbers.
        pw.println(rnd.nextInt() + " " + rnd.nextInt());
    }
    
    @edu.sharif.cs.contests.OutputTest
    public void solve(java.util.Scanner reader, java.io.PrintWriter writer) {
        // solve the problem in this function.
        // read the file from reader and write the solution into writer.
        writer.println(reader.nextInt() + reader.nextInt());
    }
    
    @edu.sharif.cs.contests.Destroy
    public void tearDown() {
        // this function will be invoked after all tests are finished.
        // or the invoking threads don't respond for at least 5 minutes. :/
    }
}
```

**Note:**
1. This library does not guarantee orders of input test cases (or sample inputs).
2. It detects appropriate functions to call by annotations. Names don't matter but input parameters do.
3. You can remove any functions you don't want to use.
4. The methods should be public. They can be inherited from super types.
5. Return type of methods don't matter.

### Step 2
Now you can generate your test files with Java code:
```java
public class Main {
    public static void main(String[] args) {
        edu.sharif.cs.contests.Generator.generate(
                Sum.class,
                /* optional output dir for tests */ "sum",
                /* optional number of test cases */ 100);
    }
}
``` 

Or you can use command line to generate them
(A default main method is available in Generator class)
```
$ java edu.sharif.cs.contests.Generator fully.qualified.class.Name path/to/output/dir 100
```

**Note:**
1. If you don't provide the value for output dir, it will default to `"/test-cases/" + class.getSimpleName().toLowerCase()`.
2. If you don't provide the value for number of tests, it will default to 100.
