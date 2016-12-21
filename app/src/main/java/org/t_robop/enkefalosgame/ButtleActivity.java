package org.t_robop.enkefalosgame;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
    int residue=5;

    //各々の札の有無(例：3を出したら 11011)
    int playerAllCards=11111;
    int botAllCards=11111;

    int playerWin=0;
    int botWin=0;

    //開いたレコードのid退避用
    String battleId;

    //TextView's
    TextView cardsResidue;
    TextView cardsPlayer;
    TextView cardsBot;
    //button達
    Button playButton[]=new Button[5];

    //勝敗分判定
    int judge;

    boolean angel=false;

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

        cardsResidue=(TextView)findViewById(R.id.bot_remain);
        cardsPlayer=(TextView)findViewById(R.id.player_card);
        cardsBot=(TextView)findViewById(R.id.bot_card);

        playButton[0]=(Button)findViewById(R.id.button_0);
        playButton[1]=(Button)findViewById(R.id.button_1);
        playButton[2]=(Button)findViewById(R.id.button_2);
        playButton[3]=(Button)findViewById(R.id.button_3);
        playButton[4]=(Button)findViewById(R.id.button_4);

    }

    //ボタンが押された時
    public void choise(View v){

        /*****表面処理*****/
        //タグから押された札を取得
        String temp=String.valueOf(v.getTag());
        playerBattleCard=Integer.parseInt(temp);
        //＆表示
        cardsPlayer.setText(String.valueOf(playerBattleCard));
        //Buttonを見えなくする
        playButton[playerBattleCard].setVisibility(View.INVISIBLE);

        //札数を減らす
        residue--;
        //＆表示
        cardsResidue.setText("残："+String.valueOf(residue));

        /*****戦闘処理*****/
        //データベース検索とbotの出し手の決定
        searchNCMB(playerAllCards,botAllCards,"battle");

    }

    public void battle(){

        if(angel) {
            searchNCMB(playerAllCards, botAllCards, "battle");
        }
        angel=false;

        //戦闘
        //引き分け
        if(playerBattleCard==botBattleCard){
            //draw
            judge=2;
        }
        //ユーザーの逆転勝利
        else if(playerBattleCard==0&&botBattleCard==4){
            //win
            playerWin++;
            judge=0;
        }
        //botの逆転勝利
        else if(botBattleCard==0&&playerBattleCard==4){
            //lose
            botWin++;
            judge=1;
        }
        //普通にユーザーの勝ち
        else if(playerBattleCard>botBattleCard){
            //win
            playerWin++;
            judge=0;
        }
        //普通にbotの勝ち
        else if(botBattleCard>playerBattleCard){
            //lose
            botWin++;
            judge=1;
        }

        battleFinish();

    }

    //戦闘終了
    public void battleFinish(){
        /*****戦闘終了処理*****/

        //bot学習
        if(battleId!=null) {
            Log.d("NIFTY", battleId);
        }
        //bot勝利時
        if(judge==1){
            //勝ったパターンを記録
            editRecord(playerAllCards,botAllCards,String.valueOf(botBattleCard));

            //残りの札を形式に変えて代入
            playerAllCards=renovationMethod(playerAllCards,playerBattleCard);
            botAllCards=renovationMethod(botAllCards,botBattleCard);
        }
        //bot敗北時
        else if(judge==0){
            //player(勝者)のデータを取得
            searchNCMB(botAllCards,playerAllCards,"get");
        }
    }

    //データ取得からの編集
    public void tempBattle(){
        if(angel) {
            searchNCMB(botAllCards, playerAllCards, "");
        }
        angel=false;

        //相手のパターンを記録
        editRecord(botAllCards,playerAllCards,String.valueOf(playerBattleCard));

        //残りの札を形式に変えて代入
        playerAllCards=renovationMethod(playerAllCards,playerBattleCard);
        botAllCards=renovationMethod(botAllCards,botBattleCard);
    }

    //検索関数
    public void searchNCMB(final int playerValue, final int botValue, final String mode){

        //TestClassを検索するためのNCMBQueryインスタンスを作成
        NCMBQuery<NCMBObject> query = new NCMBQuery<>("Data");

        //データを検索する条件を設定
        query.whereEqualTo("botCards", botValue);
        //query.whereEqualTo("enemyCards", playerValue);

        //データストアからデータを検索
        query.findInBackground(new FindCallback<NCMBObject>() {
            @Override
            public void done(List<NCMBObject> results, NCMBException e) {
                if (e != null) {

                    //検索失敗時の処理

                    //addRecord(playerValue,botValue);

                } else {
                    //検索成功時の処理
                    if(results.size()!=0) {

                        int temp=0;

                        for(int i=0;i<results.size();i++){
                            if(results.get(i).getInt("enemyCards")==playerValue){
                                temp=i;
                                break;
                            }
                        }

                        //条件に合うレコードのidを取得
                        battleId = results.get(temp).getObjectId();

                        if (mode.equals("battle")) {
                            //botの出し手の取得
                            botBattleCard = getBotCard(results,temp);
                            battle();
                        }
                    }
                    else {
                        if(mode.equals("get")) {
                            tempBattle();
                        }
                        else {
                            addRecord(playerValue, botValue);
                            angel = true;
                            battle();
                        }
                    }
                }
            }
        });

    }

    //botの出し手の決定
    public int getBotCard(List<NCMBObject> results,int pos){
        //勝数一時退避用
        double temp=0;
        //より強い手の一時退避
        int dicision=0;
        //読み込み用配列の作成(拡張性上げるためにとりあえずこのままで)
        double card[]=new double[5];
        //5回やってより強い出し手へと洗礼していく
        for(int i=0;i<5;i++){
            if(judgeMethod(botAllCards,i)) {
                //データの取得
                card[i] = results.get(pos).getDouble(String.valueOf(i));
                //より高い勝数の手を出す(とりあえず乱数は無しで)
                if (card[i] >= temp) {
                    temp = card[i];
                    dicision = i;
                }
            }
        }
        //現状最強の出し手を君臨させる
        return dicision;
    }

    //レコードの編集
    public void editRecord(int playerValue, int botValue,String value){

        final NCMBObject obj = new NCMBObject("Data");
        obj.put("botCards", botValue);
        obj.put("enemyCards", playerValue);
        obj.put(value,+1);
        //idセット
        obj.setObjectId(battleId);

        obj.saveInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e != null) {

                    //エラー発生時の処理
                } else {

                    //成功時の処理
                    Log.d("NIFTY","edit");
                }
            }
        });
    }

    //レコード追加
    public void addRecord(int playerValue, int botValue){
        NCMBObject obj = new NCMBObject("Data");
        obj.put("botCards", botValue);
        obj.put("enemyCards", playerValue);

        obj.saveInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e != null) {

                    //エラー発生時の処理
                } else {

                    //成功時の処理
                    Log.d("NIFTY","add");
                }
            }
        });
    }

    //出した数によって残りの札を変えて返す関数
    public int renovationMethod(int origin,int card){

        int blackDog=1;

        for(int i=0;i<card;i++){
            blackDog=blackDog*10;
        }

        return origin-blackDog;
    }

    //持ち札の中に指定カードがあるか否かの確認用関数
    public boolean judgeMethod(int origin,int card){

        int savior;
        int cal=origin;

        for(int i=0;i<card;i++){
            cal=cal/10;
        }

        savior=cal%10;

        if(savior==1){
            return true;
        }

        return false;
    }
}
