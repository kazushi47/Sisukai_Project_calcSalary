import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 給与計算クラス
 *
 * 使用方法などの説明は以下のリンクから。
 * https://github.com/kazushi47/Sisukai_Project_calcSalary
 * 
 * @since 2021/01/07
 * @version 1.0
 * @author kazushi47
 */
public class CalcSalary {
    /** 給与計算対象部署ID */
    private final int DIV_ID;
    /** 給与計算対象期間の開始日("yyyy-mm-dd"の形の文字列) */
    private final String START_DATE;
    /** 給与計算対象期間の終了日("yyyy-mm-dd"の形の文字列) */
    private final String END_DATE;

    /** 計算結果の格納用 */
    private List<Map<String, Integer>> results = new ArrayList<>();
    /** データベース接続用Connection */
    private Connection connection = DBconnect.getConnection();

    /**
     * 当クラスを使用するにはこのコンストラクタを使用しインスタンス化する必要がある。
     * 
     * @param DIV_ID     給与計算対象部署ID
     * @param START_DATE 給与計算対象期間の開始日("yyyy-mm-dd"の形の文字列)
     * @param END_DATE   給与計算対象期間の終了日("yyyy-mm-dd"の形の文字列)
     * @throws SQLException データベースのSalaryテーブル接続例外
     */
    public CalcSalary(int DIV_ID, String START_DATE, String END_DATE) throws SQLException {
        /* パラメータの設定 */
        this.DIV_ID = DIV_ID;
        this.START_DATE = START_DATE;
        this.END_DATE = END_DATE;
        
        /* Salaryテーブルのカラム一覧をresult_paramsに設定 */
        List<String> result_params = new ArrayList<>();
        Statement stForResults = connection.createStatement();
        ResultSet rsForResults = stForResults.executeQuery("select * from salarys");
        ResultSetMetaData meta = rsForResults.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            result_params.add(meta.getColumnName(i));
        }
        rsForResults.close();
        stForResults.close();

        /* 対象社員IDの計算結果格納用resultsの設定 */
        PreparedStatement psForEmpIds = connection.prepareStatement("select empId from employees where divId = ?");
        psForEmpIds.setInt(1, this.DIV_ID);
        ResultSet rsForEmpIds = psForEmpIds.executeQuery();
        while (rsForEmpIds.next()) {
            Map<String, Integer> work = new LinkedHashMap<>();

            /* 計算結果のkey値をresult_paramsの値に設定 */
            result_params.forEach(s -> {
                work.put(s, 0);
            });
            /* 計算結果のkey値であるempIdに社員IDを設定 */
            work.put("empId", rsForEmpIds.getInt(1));
            /* 計算結果のkey値であるdateは使用しないので削除する */
            work.remove("date");

            results.add(work);
        }
        rsForEmpIds.close();
        psForEmpIds.close();
    }

    /**
     * 対象部署・期間の給与計算を実行し、結果をデータベースに格納するにはこれを呼び出す。
     * 
     * @param isFirstCalc 最初の給与計算の場合はTrueに設定する。2回目以降の再計算の場合はFalseに設定する。
     * @throws SQLException データベース取得・登録時に発生する例外
     */
    public void executeCalc(boolean isFirstCalc) throws SQLException {
        calculate();
        postDatabase(isFirstCalc);
    }

    /**
     * 対象部署・期間の給与計算を実行する。
     * 
     * @throws SQLException データベース取得時に発生する例外
     */
    public void calculate() throws SQLException {
        for (Map<String, Integer> result : results) {
            int empId = result.get("empId");
            /* 年齢給 */
            result.put("ageSalary", calcAgeSalary(empId));
            /* 職能給 */
            result.put("abilitySalary", calcAbilitySalary(empId));
            /* 役職手当 */
            result.put("jobTitleSalary", calcJobTitleSalary(empId));
            /* 特務手当 */
            result.put("specialWorkSalary", calcSpecialWorkSalary(empId));
            /* 調整手当 */
            result.put("controlSalary", calcControlSalary(empId));
            /* 通勤手当 */
            result.put("commuteSalary", calcCommuteSalary(empId));
            /* 出張手当 */
            result.put("businessTripSalary", calcBusinessTripSalary(empId));
            /* 時間外勤務割増給与 */
            result.put("overWorkSalary", calcOverWorkSalary(empId));
            /* 休日勤務割増給与 */
            result.put("holidayWorkSalary", calcHolidayWorkSalary(empId));
            /* 深夜勤務割増給与 */
            result.put("nightWorkingSalary", calcNightWorkingSalary(empId));
            /* 特別休暇給与 */
            result.put("specialHolidaySalary", calcSpecialHolidaySalary(empId));
            /* 控除額 */
            result.put("deduction", calcDeduction(empId));
            /* 時間外勤務時数 */
            result.put("overWorkTime", calcOverWorkTime(empId));
            /* 休日勤務時数 */
            result.put("holidayWorkTime", calcHolidayWorkTime(empId));
            /* 深夜勤務時数 */
            result.put("nightWorkTime", calcnightWorkTime(empId));
            /* 対象特別休暇日数 */
            result.put("targetspecialHolidays", calcTargetSpecialHolidays(empId));
            /* 非就業時間 */
            result.put("notWorkTime", calcNotWorkTime(empId));
            /* 有給休暇日数 */
            result.put("paidHolidays", calcPaidHolidays(empId));
        }
    }

    /**
     * 計算結果をデータベースに格納する。
     * 最初の給与計算の場合は、まずsalarysに対してInsertを実行する。この時点ではempIdとdateのみデータを格納する。
     * その後にsalarysに対してUpdateを実行する。計算結果であるresultsにある値でデータベースを更新していく。
     * なお、2回目以降の再計算の場合は最初のInsertは行わず、Updateで更新のみ行う。
     * 
     * @param isFirstCalc 最初の給与計算の場合はTrueに設定する。2回目以降の再計算の場合はFalseに設定する。
     * @throws SQLException データベース登録時に発生する例外
     */
    private void postDatabase(boolean isFirstCalc) throws SQLException {
        /* Insertで初期化 */
        if (isFirstCalc) {
            PreparedStatement psForInsert = connection.prepareStatement("insert into salarys(empId, date) values(?, ?)");
            for (Map<String, Integer> result : results) {
                /* パラメータのクリア */
                psForInsert.clearParameters();
                /* パラメータの設定 */
                psForInsert.setInt(1, result.get("empId"));
                psForInsert.setDate(2, Date.valueOf(START_DATE));
                psForInsert.executeUpdate();
            }
            psForInsert.close();
        }
        /* Updateで登録 */
        for (Map<String, Integer> result : results) {
            for (Map.Entry<String, Integer> entry : result.entrySet()) {
                PreparedStatement psForUpdate = connection.prepareStatement("update salarys set " + entry.getKey() + " = ? where empId = ? and date = ?");
                
                /* パラメータのクリア */
                psForUpdate.clearParameters();
                /* パラメータの設定 */
                psForUpdate.setInt(1, entry.getValue());
                psForUpdate.setInt(2, result.get("empId"));
                psForUpdate.setDate(3, Date.valueOf(START_DATE));
                psForUpdate.executeUpdate();
    
                psForUpdate.close();
            }
        }
    }

    /**
     * 年齢給
     * 
     * @param empId 対象社員ID
     * @return 年齢給
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcAgeSalary(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select salary from ageSalarys where age in("
            + "select iif(format(?, \"mmdd\") < format(birthdate, \"mmdd\"), datediff(\"yyyy\", birthdate, ?) - 1, datediff(\"yyyy\", birthdate, ?))"
            + " from employees where empId = ?)"
        );
        ps.setDate(1, Date.valueOf(START_DATE));
        ps.setDate(2, Date.valueOf(START_DATE));
        ps.setDate(3, Date.valueOf(START_DATE));
        ps.setInt(4, empId);
        ResultSet rs = ps.executeQuery();
        int ageSalary = 0;
        if (rs.next()) {
            ageSalary = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return ageSalary;
    }

    /**
     * 職能給
     * 
     * @param empId 対象社員ID
     * @return 職能給
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcAbilitySalary(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select salary from abilitySalarys where abilityGrade in(select abilityGrade from employees where empId = ?)"
        );
        ps.setInt(1, empId);
        ResultSet rs = ps.executeQuery();
        int abilitySalary = 0;
        if (rs.next()) {
            abilitySalary = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return abilitySalary;
    }

    /**
     * 役職手当
     * 
     * @param empId 対象社員ID
     * @return 役職手当
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcJobTitleSalary(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select salary from jobTitleSalarys where jobTitleGrade in(select jobTitleGrade from employees where empId = ?)"
        );
        ps.setInt(1, empId);
        ResultSet rs = ps.executeQuery();
        int jobTitleSalary = 0;
        if (rs.next()) {
            jobTitleSalary = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return jobTitleSalary;
    }

    /**
     * 特務手当
     * 
     * @param empId 対象社員ID
     * @return 特務手当
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcSpecialWorkSalary(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select salary from specialWorkSalarys where specialWorkGrade in(select specialWorkGrade from employees where empId = ?)"
        );
        ps.setInt(1, empId);
        ResultSet rs = ps.executeQuery();
        int specialWorkSalary = 0;
        if (rs.next()) {
            specialWorkSalary = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return specialWorkSalary;
    }

    /**
     * 調整手当
     * 
     * @param empId 対象社員ID
     * @return 調整手当
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcControlSalary(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select sum(salary) from controlSalarys where empId = ? and startDate between ? and ?"
        );
        ps.setInt(1, empId);
        ps.setDate(2, Date.valueOf(START_DATE));
        ps.setDate(3, Date.valueOf(END_DATE));
        ResultSet rs = ps.executeQuery();
        int controlSalary = 0;
        if (rs.next()) {
            controlSalary = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return controlSalary;
    }

    /**
     * 通勤手当
     * 
     * @param empId 対象社員ID
     * @return 通勤手当
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcCommuteSalary(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select sum(salary) from commuteSalarys where empId = ? and startDate between ? and ?"
        );
        ps.setInt(1, empId);
        ps.setDate(2, Date.valueOf(START_DATE));
        ps.setDate(3, Date.valueOf(END_DATE));
        ResultSet rs = ps.executeQuery();
        int commuteSalary = 0;
        if (rs.next()) {
            commuteSalary = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return commuteSalary;
    }

    /**
     * 出張手当
     * 
     * @param empId 対象社員ID
     * @return 出張手当
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcBusinessTripSalary(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select sum(b.salary) from "
            + "(select w1.myId as targetId, max(b.targetJobTitleGrade) as targetGrade from "
                + "(select e.empId as myId, iif(e.jobTitleGrade is null, 0, e.jobTitleGrade) as myGrade from employees e where e.empId = ?) w1, "
                + "businessTripSalarys b where w1.myGrade >= b.targetJobTitleGrade group by w1.myId) w2, "
            + "attendances a, businessTripSalarys b "
            + "where w2.targetId = a.empId and w2.targetGrade = b.targetJobTitleGrade and a.businessTripType = b.businessTripType "
            + "and a.date between ? and ?"
        );
        ps.setInt(1, empId);
        ps.setDate(2, Date.valueOf(START_DATE));
        ps.setDate(3, Date.valueOf(END_DATE));
        ResultSet rs = ps.executeQuery();
        int businessTripSalary = 0;
        if (rs.next()) {
            businessTripSalary = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return businessTripSalary;
    }

    /**
     * 時間外勤務割増給与
     * 
     * @param empId 対象社員ID
     * @return 時間外勤務割増給与
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcOverWorkSalary(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select fix(iif(diff > 0, ? / 157.5 * 1.25 * diff, 0)) "
            + "from (select sum(fix(datediff(\"n\", attendanceTime, leavingTime) / 30) / 2.0) - 157.5 as diff from attendances where empId = ? and date between ? and ? and attendanceTime is not null and leavingTime is not null)"
        );
        ps.setInt(1, calcAgeSalary(empId) + calcAbilitySalary(empId));  // 基本給をセット
        ps.setInt(2, empId);
        ps.setDate(3, Date.valueOf(START_DATE));
        ps.setDate(4, Date.valueOf(END_DATE));
        ResultSet rs = ps.executeQuery();
        int overWorkSalary = 0;
        if (rs.next()) {
            overWorkSalary = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return overWorkSalary;
    }

    /**
     * 休日勤務割増給与
     * 
     * @param empId 対象社員ID
     * @return 休日勤務割増給与
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcHolidayWorkSalary(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select fix(? / 157.5 * 0.1 * sum(iif(work >= 2.0, work, 0))) "
            + "from (select fix(datediff(\"n\", attendanceTime, leavingTime) / 30) / 2.0 as work from attendances "
            + "where (weekday(date) in(1, 7) or date in(select date from holidays) or format(date, \"mm/dd\") in(\"12/29\", \"12/30\", \"12/31\", \"01/01\", \"01/02\", \"01/03\", \"01/04\")) "
            + "and empId = ? and date between ? and ? and attendanceTime is not null and leavingTime is not null)"
        );
        ps.setInt(1, calcAgeSalary(empId) + calcAbilitySalary(empId));  // 基本給をセット
        ps.setInt(2, empId);
        ps.setDate(3, Date.valueOf(START_DATE));
        ps.setDate(4, Date.valueOf(END_DATE));
        ResultSet rs = ps.executeQuery();
        int holidayWorkSalary = 0;
        if (rs.next()) {
            holidayWorkSalary = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return holidayWorkSalary;
    }

    /**
     * 深夜勤務割増給与
     * 
     * @param empId 対象社員ID
     * @return 深夜勤務割増給与
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcNightWorkingSalary(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select fix(? / 157.5 * 0.25 * "
            + "sum(fix((iif(attendanceTime <= #4:30:0#, datediff(\"n\", attendanceTime, #5:0:0#), 0) + iif(leavingTime >= #22:30:0#, datediff(\"n\", #22:0:0#, leavingTime), 0) + iif(leavingTime <= #5:0:0#, datediff(\"n\", #0:0:0#, leavingTime), 0)) / 30) /2.0)) "
            + "from attendances where empId = ? and date between ? and ? and attendanceTime is not null and leavingTime is not null"
        );
        ps.setInt(1, calcAgeSalary(empId) + calcAbilitySalary(empId));  // 基本給をセット
        ps.setInt(2, empId);
        ps.setDate(3, Date.valueOf(START_DATE));
        ps.setDate(4, Date.valueOf(END_DATE));
        ResultSet rs = ps.executeQuery();
        int nightWorkingSalary = 0;
        if (rs.next()) {
            nightWorkingSalary = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return nightWorkingSalary;
    }

    /**
     * 特別休暇給与
     * 
     * @param empId 対象社員ID
     * @return 特別休暇給与
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcSpecialHolidaySalary(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select fix(count(*) * ? * 60 / 100) from attendances where specialHolidayType = \"臨時休業\" and empId = ? and date between ? and ?"
        );
        // 平均給与日額をセット
        ps.setInt(1, (calcAgeSalary(empId) + calcAbilitySalary(empId) + calcJobTitleSalary(empId) + calcSpecialWorkSalary(empId) + calcControlSalary(empId) + calcCommuteSalary(empId) + calcBusinessTripSalary(empId) + calcOverWorkSalary(empId) + calcHolidayWorkSalary(empId) + calcNightWorkingSalary(empId)) / 21);
        ps.setInt(2, empId);
        ps.setDate(3, Date.valueOf(START_DATE));
        ps.setDate(4, Date.valueOf(END_DATE));
        ResultSet rs = ps.executeQuery();
        int specialHolidaySalary = 0;
        if (rs.next()) {
            specialHolidaySalary = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return specialHolidaySalary;
    }

    /**
     * 控除額
     * 
     * @param empId 対象社員ID
     * @return 控除額
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcDeduction(int empId) throws SQLException {
        // TODO 控除額の計算
        return 0;
    }

    /**
     * 時間外勤務時数
     * 
     * @param empId 対象社員ID
     * @return 時間外勤務時数
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcOverWorkTime(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select iif(diff > 0, diff, 0) "
            + "from (select sum(fix(datediff(\"n\", attendanceTime, leavingTime) / 30) / 2.0) - 157.5 as diff from attendances where empId = ? and date between ? and ? and attendanceTime is not null and leavingTime is not null)"
        );
        ps.setInt(1, empId);
        ps.setDate(2, Date.valueOf(START_DATE));
        ps.setDate(3, Date.valueOf(END_DATE));
        ResultSet rs = ps.executeQuery();
        int overWorkTime = 0;
        if (rs.next()) {
            overWorkTime = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return overWorkTime;
    }

    /**
     * 休日勤務時数
     * 
     * @param empId 対象社員ID
     * @return 休日勤務時数
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcHolidayWorkTime(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select fix(sum(iif(work >= 2.0, work, 0))) "
            + "from (select fix(datediff(\"n\", attendanceTime, leavingTime) / 30) / 2.0 as work from attendances "
            + "where (weekday(date) in(1, 7) or date in(select date from holidays) or format(date, \"mm/dd\") in(\"12/29\", \"12/30\", \"12/31\", \"01/01\", \"01/02\", \"01/03\", \"01/04\")) "
            + "and empId = ? and date between ? and ? and attendanceTime is not null and leavingTime is not null)"
        );
        ps.setInt(1, empId);
        ps.setDate(2, Date.valueOf(START_DATE));
        ps.setDate(3, Date.valueOf(END_DATE));
        ResultSet rs = ps.executeQuery();
        int holidayWorkTime = 0;
        if (rs.next()) {
            holidayWorkTime = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return holidayWorkTime;
    }

    /**
     * 深夜勤務時数
     * 
     * @param empId 対象社員ID
     * @return 深夜勤務時数
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcnightWorkTime(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select fix( "
            + "sum(fix((iif(attendanceTime <= #4:30:0#, datediff(\"n\", attendanceTime, #5:0:0#), 0) + iif(leavingTime >= #22:30:0#, datediff(\"n\", #22:0:0#, leavingTime), 0) + iif(leavingTime <= #5:0:0#, datediff(\"n\", #0:0:0#, leavingTime), 0)) / 30) /2.0)) "
            + "from attendances where empId = ? and date between ? and ? and attendanceTime is not null and leavingTime is not null"
        );
        ps.setInt(1, empId);
        ps.setDate(2, Date.valueOf(START_DATE));
        ps.setDate(3, Date.valueOf(END_DATE));
        ResultSet rs = ps.executeQuery();
        int nightWorkingTime = 0;
        if (rs.next()) {
            nightWorkingTime = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return nightWorkingTime;
    }

    /**
     * 対象特別休暇日数
     * 
     * @param empId 対象社員ID
     * @return 対象特別休暇日数
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcTargetSpecialHolidays(int empId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "select count(*) from attendances where specialHolidayType = \"臨時休業\" and empId = ? and date between ? and ?"
        );
        // 平均給与日額をセット
        ps.setInt(1, empId);
        ps.setDate(2, Date.valueOf(START_DATE));
        ps.setDate(3, Date.valueOf(END_DATE));
        ResultSet rs = ps.executeQuery();
        int specialHolidays = 0;
        if (rs.next()) {
            specialHolidays = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return specialHolidays;
    }

    /**
     * 非就業時間
     * 
     * @param empId 対象社員ID
     * @return 非就業時間
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcNotWorkTime(int empId) throws SQLException {
        // TODO 非就業時間
        // PreparedStatement ps = connection.prepareStatement(
        //     "SELECT w1+w2 FROM (SELECT count(*) AS w1 FROM attendances WHERE empId = ? and absence = \"〇\" and paidHoliday = \"\" and specialHolidayType is null and [date] between ? and ?) AS t1, (SELECT sum(iif(fix(datediff(\"n\", attendanceTime, #11:0:0#)/30)/2.0>2.75, 1, iif(fix(datediff(\"n\", attendanceTime, #11:0:0#)/30)/2.0>1.75, 0.5, 0)) + iif(fix(datediff(\"n\", #15:30:0#, leavingTime)/30)/2.0>2.75, 1, iif(fix(datediff(\"n\", #15:30:0#, leavingTime)/30)/2.0>1.75, 0.5, 0))) + fix(sum(iif(datediff(\"h\", attendanceTime, #11:0:0#)<=1, 1, 0))/3) + fix(sum(iif(datediff(\"h\", #15:30:0#, leavingTime)<=1, 1, 0))/3) AS w2 FROM attendances WHERE empId = ? and paidHoliday = \"\" and specialHolidayType is null and [date] between ? and ?) AS t2"
        // );
        // ps.setInt(1, empId);
        // ps.setDate(2, Date.valueOf(START_DATE));
        // ps.setDate(3, Date.valueOf(END_DATE));
        // ps.setInt(4, empId);
        // ps.setDate(5, Date.valueOf(START_DATE));
        // ps.setDate(6, Date.valueOf(END_DATE));
        // ResultSet rs = ps.executeQuery();
        int notWorkTime = 0;
        // if (rs.next()) {
        //     notWorkTime = rs.getInt(1);
        // }
        // rs.close();
        // ps.close();
        return notWorkTime;
    }

    /**
     * 有給休暇日数
     * 
     * @param empId 対象社員ID
     * @return 有給休暇日数
     * @throws SQLException データベース取得時に発生する例外
     */
    public int calcPaidHolidays(int empId) throws SQLException {
        // TODO 有給休暇日数
        return 0;
    }

    @Override
    public String toString() {
        return "CalcSalary [DIV_ID=" + DIV_ID + ", END_DATE=" + END_DATE + ", START_DATE=" + START_DATE + ", results="
                + results.toString() + "]";
    }
}
