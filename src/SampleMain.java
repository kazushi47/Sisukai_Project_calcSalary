
public class SampleMain {
    public static void main(String[] args) throws Exception {
        CalcSalary calcSalary = new CalcSalary(7, "START_DATE", "END_DATE");
        calcSalary.executeCalc();
        System.out.println(calcSalary);
    }
}
