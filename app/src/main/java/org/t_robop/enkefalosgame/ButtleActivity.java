package org.t_robop.enkefalosgame;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
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
    //再検索判定
    boolean angel=false;

    //サーバーデータ一時保存用変数
    NCMBObject gloObj;

    //処理中か否かの判定
    boolean systemLoading=false;

    //勝敗を出すダイアログ
    AlertDialog.Builder alertDialog;

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

        //処理中は押せません
        if(systemLoading==false) {

            systemLoading=true;

            /*****表面処理*****/
            //タグから押された札を取得
            String temp = String.valueOf(v.getTag());
            playerBattleCard = Integer.parseInt(temp);
            //＆表示
            cardsPlayer.setText(String.valueOf(playerBattleCard));
            //Buttonを見えなくする
            playButton[playerBattleCard].setVisibility(View.INVISIBLE);

            //札数を減らす
            residue--;
            //＆表示
            //cardsResidue.setText("残：" + String.valueOf(residue));

            /*****戦闘処理*****/
            //データベース検索とbotの出し手の決定
            searchNCMB(playerAllCards, botAllCards, "battle");

        }

    }

    public void battle(){

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
            //サーバーの勝数を取得
            int winNum=gloObj.getInt(String.valueOf(botBattleCard));
            //戦闘数の記録
            int num=gloObj.getInt(String.valueOf(botBattleCard)+"num");
            //勝ったパターンを記録
            editRecord(playerAllCards,botAllCards,String.valueOf(botBattleCard),num,winNum+1);

            //player(敗者)のデータを取得
            searchNCMB(botAllCards,playerAllCards,"getL");
        }
        //bot敗北時
        else if(judge==0){

            //サーバーの勝数を取得
            int winNum=gloObj.getInt(String.valueOf(botBattleCard));
            //戦闘数の記録
            int num=gloObj.getInt(String.valueOf(botBattleCard)+"num");
            //勝ったパターンを記録
            editRecord(playerAllCards,botAllCards,String.valueOf(botBattleCard),num,winNum);

            //player(勝者)のデータを取得
            searchNCMB(botAllCards,playerAllCards,"getW");
        }
        //引き分け時
        else if(judge==2){
            //サーバーの引き分け数を取得
            int num=gloObj.getInt("draw");
            //引き分けパターンを記録
            editRecord(playerAllCards,botAllCards,"draw",num,0);

            //残りの札を形式に変えて代入
            playerAllCards=renovationMethod(playerAllCards,playerBattleCard);
            botAllCards=renovationMethod(botAllCards,botBattleCard);

            //表示
            cardsResidue.setText("" + String.valueOf(botAllCards));
            //ダイアログ表示
            drawDialog();
            //処理おわり
            systemLoading=false;
        }
    }

    //避難所
    public void tempBattle(int type){
        //バトル終了の学習
        if(type==1||type==0) {
            if (angel) {
                searchNCMB(botAllCards, playerAllCards, "");
            }
            angel = false;

            //todo 勝数じゃなくて勝率取ってきてるから勝率を内部で計算させればいいんや
            //サーバーの勝数を取得
            int winNum=gloObj.getInt(String.valueOf(playerBattleCard));
            //戦闘数の記録
            int num=gloObj.getInt(String.valueOf(playerBattleCard)+"num");
            if(type==1) {
                //相手のパターンを記録
                editRecord(botAllCards, playerAllCards, String.valueOf(playerBattleCard), num, winNum );
            }
            else {
                //相手のパターンを記録
                editRecord(botAllCards, playerAllCards, String.valueOf(playerBattleCard), num, winNum +1);
            }

            //残りの札を形式に変えて代入
            playerAllCards = renovationMethod(playerAllCards, playerBattleCard);
            botAllCards = renovationMethod(botAllCards, botBattleCard);

            //表示
            cardsResidue.setText("" + String.valueOf(botAllCards));
            //ダイアログ表示
            drawDialog();
            //処理おわり
            systemLoading=false;
        }
        //バトル直前の再検索処理
        else if(type==2){
            if(angel) {
                searchNCMB(playerAllCards, botAllCards, "battle");
            }
            angel=false;
        }
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

                        gloObj=results.get(temp);

                        if (mode.equals("battle")) {
                            //botの出し手の取得
                            botBattleCard = getBotCard(results,temp);
//                            cardsBot.setText(botBattleCard);
                            battle();
                        }
                        else if(mode.equals("getL")) {
                            //バトル終了の学習に飛ばす
                            tempBattle(1);
                        }
                        else if(mode.equals("getW")){
                            //バトル終了の学習に飛ばす
                            tempBattle(0);
                        }
                    }
                    else {
                        if(mode.equals("getL")) {
                            //バトル終了の学習に飛ばす
                            tempBattle(1);
                        }
                        else if(mode.equals("getW")){
                            //バトル終了の学習に飛ばす
                            tempBattle(0);
                        }
                        else {
                            addRecord(playerValue, botValue);
                            angel = true;
                            //バトル直前の再検索に飛ばす
                            tempBattle(2);
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
        //洗礼用
        double per;
        //読み込み用
        int win;
        int num;
        //5回やってより強い出し手へと洗礼していく
        for(int i=0;i<5;i++){
            if(judgeMethod(botAllCards,i)) {
                //データの取得
                win = results.get(pos).getInt(String.valueOf(i));
                num= results.get(pos).getInt(String.valueOf(i)+"num");
                per=getWinPer(win,num);
                //より高い勝数の手を出す(とりあえず乱数は無しで)
                if (per >= temp) {
                    temp = per;
                    dicision = i;
                }
            }
        }
        //現状最強の出し手を君臨させる
        return dicision;
    }

    //勝率の計算
    public double getWinPer(int winNum,int battleNum){
        double per=0;
        if(battleNum!=0) {
            per = (winNum / battleNum) * 100;
        }
        return per;
    }

    //レコードの編集
    public void editRecord(int playerValue, int botValue,String value,int gloNum,int winNum){

        final NCMBObject obj = new NCMBObject("Data");
        obj.put("botCards", botValue);
        obj.put("enemyCards", playerValue);
        if(!value.equals("draw")) {
            obj.put(value, winNum);
            obj.put(value+"num",gloNum+1);
        }
        else {
            obj.put(value , gloNum + 1);
        }
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

        for(int i=1;i<=card;i++){
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

    public void setDialog(String title, String massage, final String mode){

        alertDialog=new AlertDialog.Builder(this);

        // ダイアログの設定
        alertDialog.setTitle(title);      //タイトル設定
        alertDialog.setMessage(massage);  //内容(メッセージ)設定

        // OK(肯定的な)ボタンの設定
        alertDialog.setPositiveButton("次へ", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // OKボタン押下時の処理
                if(mode.equals("end")){
                    Intent intent=new Intent(ButtleActivity.this,MainActivity.class);
                    startActivity(intent);
                }
                else{
                    cardsBot.setText("？");
                }
            }
        });

        alertDialog.show();

    }

    public void drawDialog(){

        String title="";
        String masse="";
        String mode="";

        //手札０(ゲーム終了時)
        if(residue==0){
            title="ゲーム終了";
            mode="end";
            if(playerWin==botWin){
                masse="引き分け：";
            }
            else if (playerWin > botWin) {
                masse="ユーザー勝利：";
            }
            else if(botWin>playerWin){
                masse="CPU勝利：";
            }
            masse=masse+playerWin+"-"+botWin;
        }
        else {
            if(judge==0){
                title="ユーザーの勝利";
            }
            else if(judge==1){
                title="CPUの勝利";
            }
            else if(judge==2){
                title="引き分け";
            }
            masse=playerBattleCard+"-"+botBattleCard;
        }

        setDialog(title,masse,mode);

    }
}
