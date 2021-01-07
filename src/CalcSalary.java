import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 給与計算クラス
 * 
 * @since 2021/01/07
 * @version 1.0
 * @author Kazushi Sugitani
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
        for (int i = 1; i < meta.getColumnCount(); i++) {
            result_params.add(meta.getColumnName(i));
        }
        rsForResults.close();
        stForResults.close();

        /* 対象社員IDの計算結果格納用resultsの設定 */
        PreparedStatement psForEmpIds = connection.prepareStatement("select empId from employees where divId = ?");
        psForEmpIds.setInt(1, this.DIV_ID);
        ResultSet rsForEmpIds = psForEmpIds.executeQuery();
        while (rsForEmpIds.next()) {
            Map<String, Integer> work = new HashMap<>();
            result_params.forEach(s -> {
                work.put(s, 0);
            });
            work.put("empId", rsForEmpIds.getInt(1));
            results.add(work);
        }
        rsForEmpIds.close();
        psForEmpIds.close();
    }

    /**
     * 対象部署・期間の給与計算を実行し、結果をデータベースに格納するにはこれを呼び出す。
     * @throws Exception 計算処理やデータベース取得・登録時に発生する例外
     */
    public void executeCalc() throws Exception {
        calculate();
        postDatabase();
    }

    /**
     * 対象部署・期間の給与計算を実行する。
     * @throws Exception 計算処理時に発生する例外
     */
    public void calculate() throws Exception {
        
    }

    /**
     * 計算結果をデータベースに格納する。
     * @throws Exception データベース登録時に発生する例外
     */
    public void postDatabase() throws Exception {
        
    }

    @Override
    public String toString() {
        return "CalcSalary [DIV_ID=" + DIV_ID + ", END_DATE=" + END_DATE + ", START_DATE=" + START_DATE + ", results="
                + results.toString() + "]";
    }
}
