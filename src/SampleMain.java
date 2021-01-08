
public class SampleMain {
    public static void main(String[] args) throws Exception {
        CalcSalary calcSalary = new CalcSalary(7, "2020-12-21", "2021-01-20");
        calcSalary.executeCalc(true);
        System.out.println(calcSalary);
    }
}
