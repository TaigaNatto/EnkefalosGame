package org.t_robop.enkefalosgame;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.nifty.cloud.mb.core.DoneCallback;
import com.nifty.cloud.mb.core.FindCallback;
import com.nifty.cloud.mb.core.NCMB;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBQuery;

import java.util.List;

public class ButtleActivity extends AppCompatActivity {

    //各々が出した札
    int playerBattleCard;
    int botBattleCard;

    //残り札数
    int Residue=5;

    //各々の札の有無(例：3を出したら 11011)
    int playerAllCards=11111;
    int botAllCards=11111;

    int playerWin=0;
    int botWin=0;

    //開いたレコードのid退避用
    String battleId="aa";

    //button達
    Button playButton[]=new Button[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mbaas連携
        
        setContentView(R.layout.activity_buttle);

        //関連付け
        Association();

    }

    //関連付け
    public void Association(){

        playButton[0]=(Button)findViewById(R.id.button_0);
        playButton[1]=(Button)findViewById(R.id.button_1);
        playButton[2]=(Button)findViewById(R.id.button_2);
        playButton[3]=(Button)findViewById(R.id.button_3);
        playButton[4]=(Button)findViewById(R.id.button_4);

    }

    //ボタンが押された時
    public void choise(View v){

        //タグから押された札を取得
        playerBattleCard=(int)v.getTag();

    }

    //検索関数
    public void searchNCMB(int playerValue, int botValue, final String mode){

        //TestClassを検索するためのNCMBQueryインスタンスを作成
        NCMBQuery<NCMBObject> query = new NCMBQuery<>("Data");

        //データを検索する条件を設定
        query.whereEqualTo("botCards", botValue);
        query.whereEqualTo("enemyCards", playerValue);

        //データストアからデータを検索
        query.findInBackground(new FindCallback<NCMBObject>() {
            @Override
            public void done(List<NCMBObject> results, NCMBException e) {
                if (e != null) {

                    //検索失敗時の処理


                } else {
                    //検索成功時の処理

                    //条件に合うレコードのidを取得
                    battleId=results.get(0).getObjectId();

                    //戦闘用の場合
                    if(mode.equals("battle")) {
                        //botの出し手の取得
                        botBattleCard = getBotCard(results);
                    }
                }
            }
        });

    }

    //botの出し手の決定
    public int getBotCard(List<NCMBObject> results){
        //勝数一時退避用
        double temp=0;
        //より強い手の一時退避
        int dicision=0;
        //読み込み用配列の作成(拡張性上げるためにとりあえずこのままで)
        double card[]=new double[5];
        //5回やってより強い出し手へと洗礼していく
        for(int i=0;i<5;i++){
            //データの取得
            card[i]=results.get(0).getDouble(String.valueOf(i));
            //より高い勝数の手を出す(とりあえず乱数は無しで)
            if(card[i]>=temp){
                temp=card[i];
                dicision=i;
            }
        }
        //現状最強の出し手を君臨させる
        return dicision;
    }

    //レコードの追加・編集
    public void saveRecord(int playerValue, int botValue,String value,String mode){

        NCMBObject obj = new NCMBObject("Data");
        obj.put("botCards", botValue);
        obj.put("enemyCards", playerValue);

        //編集時
        if(mode.equals("edit")) {
            //idセット
            obj.setObjectId(battleId);

        }
        //追加時
        else if(mode.equals("add"))

        obj.saveInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e != null) {

                    //エラー発生時の処理
                } else {

                    //成功時の処理
                }
            }
        });

    }
}
