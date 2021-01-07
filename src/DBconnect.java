import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/**
 * DBconnect
 * MS Accessデータベースに接続するクラス。
 * 対象データベースのファイル(.accdb)のフルパス又は相対パスを定数URLに設定する必要がある。
 */
public class DBconnect {
    /** 接続失敗時メッセージ */
    private static final String W001 = "接続失敗";
    /** データベースファイルパス */
    private static final String URL  = "jdbc:ucanaccess://Database.accdb";
    
    /**
     * 指定されたデータベースに接続する。
     * 接続エラー時にはメッセージを標準出力する。
     * @return Connection型変数
     */
	public static Connection getConnection() {
        Connection con = null;
        
		try {
			con = DriverManager.getConnection(URL);
		} catch (SQLException e) {
			System.out.println(W001);
        }
        
		return con;
	}
}
