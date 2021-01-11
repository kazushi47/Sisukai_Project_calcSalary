
public class SampleMain {
    public static void main(String[] args) throws Exception {
        /* 部署ID１～７の社員の対象期間の給与を再計算するシミュレーション */
        CalcSalary calcSalary1 = new CalcSalary(1, "2020-11-21", "2020-12-20");
        CalcSalary calcSalary2 = new CalcSalary(2, "2020-11-21", "2020-12-20");
        CalcSalary calcSalary3 = new CalcSalary(3, "2020-11-21", "2020-12-20");
        CalcSalary calcSalary4 = new CalcSalary(4, "2020-11-21", "2020-12-20");
        CalcSalary calcSalary5 = new CalcSalary(5, "2020-11-21", "2020-12-20");
        CalcSalary calcSalary6 = new CalcSalary(6, "2020-11-21", "2020-12-20");
        CalcSalary calcSalary7 = new CalcSalary(7, "2020-11-21", "2020-12-20");
        calcSalary1.executeCalc(false);
        calcSalary2.executeCalc(false);
        calcSalary3.executeCalc(false);
        calcSalary4.executeCalc(false);
        calcSalary5.executeCalc(false);
        calcSalary6.executeCalc(false);
        calcSalary7.executeCalc(false);
    }
}
