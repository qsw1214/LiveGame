package com.vipheyue.livegame.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gitonway.lee.niftymodaldialogeffects.lib.Effectstype;
import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;
import com.vipheyue.livegame.R;
import com.vipheyue.livegame.bean.ConnectData;
import com.vipheyue.livegame.bean.GameBean;
import com.vipheyue.livegame.bean.MyUser;
import com.vipheyue.livegame.utils.GsonUtils;

import org.json.JSONObject;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobRealTimeData;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.ValueEventListener;


public class DisplayCCActivity extends AppCompatActivity {
    @Bind(R.id.tv_dong_total)
    TextView tv_dong_total;
    @Bind(R.id.tv_dong_Mytotal)
    TextView tv_dong_Mytotal;
    @Bind(R.id.tv_nan_total)
    TextView tv_nan_total;
    @Bind(R.id.tv_nan_Mytotal)
    TextView tv_nan_Mytotal;
    @Bind(R.id.tv_xi_total)
    TextView tv_xi_total;
    @Bind(R.id.tv_xi_Mytotal)
    TextView tv_xi_Mytotal;
    @Bind(R.id.tv_bei_total)
    TextView tv_bei_total;
    @Bind(R.id.tv_bei_Mytotal)
    TextView tv_bei_Mytotal;
    GameBean currentGameBean = new GameBean();
    @Bind(R.id.tv_userName)
    TextView tv_userName;

    @Bind(R.id.tv_userMoney)
    TextView tv_userMoney;

    private int currentSelectAmount;
    private int direction_mIn_dong;//我的下注
    private int direction_mIn_nan;
    private int direction_mIn_xi;
    private int direction_mIn_bei;

    //最多:2000+( 4个区域总和-2000)/4

    private MyUser currentUser;
    private String tempObjectId;
    private NiftyDialogBuilder dialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_cc);
        ButterKnife.bind(this);
        currentUser = BmobUser.getCurrentUser(this, MyUser.class);//这里只执行一次 因为需要操作的是临时的 currentUser 不是真实User
        updateView();
        getLatestGameBean();
    }

    /**
     * 1 更新用户金币,注意这里不能再重新获取一次 不能执行 currentUser = BmobUser.getCurrentUser(this, MyUser.class); 因为操作的是临时的user
     **/
    private void updateView() {
        tv_userName.setText("账号:" + currentUser.getUsername());
        tv_userMoney.setText("财富:" + currentUser.getMoney());
    }
    /** 2 获取服务器上最新的User 并且赋值给临时user currentUser**/
    private void getLatestGameBean() {
        currentUser = BmobUser.getCurrentUser(this, MyUser.class);
        BmobQuery<GameBean> query = new BmobQuery<GameBean>();
        query.setLimit(1); // 限制最多10条数据结果作为一页
        query.order("-updatedAt");
        query.findObjects(this, new FindListener<GameBean>() {
            @Override
            public void onSuccess(List<GameBean> object) {
                currentGameBean = object.get(0);
                tempObjectId = currentGameBean.getObjectId();
                Log.d("TestActivity", "currentGameBean.getTotalIn_dong():" + currentGameBean.getTotalIn_dong());
                LongConnectListener();
            }

            @Override
            public void onError(int code, String msg) {
                Toast.makeText(DisplayCCActivity.this, "获取最新数据错误码: " + code + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
    /** 3 监听表 实时跟新GameBean  **/
    private void LongConnectListener() {
        final BmobRealTimeData rtd = new BmobRealTimeData();
        rtd.start(this, new ValueEventListener() {
            @Override
            public void onDataChange(JSONObject data) {
                Log.d("bmob", "(" + data.optString("action") + ")" + "数据：" + data);
                ConnectData bean = GsonUtils.fromJson(data.toString(), ConnectData.class);

                currentGameBean = bean.getData();
                String leastObjId = currentGameBean.getObjectId();
                if (leastObjId.equals(tempObjectId)) {
                    Log.d("DisplayCCActivity", "obj 相等");
                } else {
                    Log.d("DisplayCCActivity", "boj 不相等");//TODO
                    // 取消监听表更新
                    rtd.unsubTableUpdate("GameBean");
                    //重新获取数据?
                    getLatestGameBean();
                    init_mIn_direction();

                }
                Log.d("bmob", bean.getData().getTotalIn_dong() + " " + bean.getData().getTotalIn_nan() + " " + bean.getData().getTotalIn_xi() + " " + bean.getData().getTotalIn_bei());
                //TODO 这里需要更新界面
                tv_dong_total.setText( currentGameBean.getTotalIn_dong() + "");
                tv_nan_total.setText( currentGameBean.getTotalIn_nan() + "");
                tv_xi_total.setText( currentGameBean.getTotalIn_xi() + "");
                tv_bei_total.setText( currentGameBean.getTotalIn_bei() + "");

                if (currentGameBean.getFinish()) {
                    //如果 finish 了 开始结算
                    int answer = currentGameBean.getAnswer();
                    int prize = 0;
                    String lotteryResult = null;
                    switch (answer) {
                        case 1:
                            prize = direction_mIn_dong * 39/10;
                            lotteryResult = "东";
                            break;
                        case 2:
                            prize = direction_mIn_nan *39/10;
                            lotteryResult = "南";
                            break;
                        case 3:
                            prize = direction_mIn_xi * 39/10;
                            lotteryResult = "西";
                            break;
                        case 4:
                            prize = direction_mIn_bei *39/10;
                            lotteryResult = "北";
                            break;
                    }
                    dialogShow(answer, lotteryResult);
                    currentUser.setMoney(currentUser.getMoney() + prize);
                    currentUser.update(DisplayCCActivity.this, new UpdateListener() {
                        @Override
                        public void onSuccess() {
                            updateView();
                        }

                        @Override
                        public void onFailure(int i, String s) {
                            Toast.makeText(DisplayCCActivity.this, "结算失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onConnectCompleted() {
                Log.d("bmob", "连接成功:" + rtd.isConnected());
                if (rtd.isConnected()) {
                    // 监听表更新
                    rtd.subTableUpdate("GameBean");
                }
            }
        });
    }


    /** 初始化我的下注**/
    private void init_mIn_direction() {
        direction_mIn_dong = 0;
        direction_mIn_nan = 0;
        direction_mIn_xi = 0;
        direction_mIn_bei = 0;
        currentSelectAmount = 0;
        tv_dong_Mytotal.setText( direction_mIn_dong+"");
        tv_nan_Mytotal.setText( direction_mIn_nan+"");
        tv_xi_Mytotal.setText(direction_mIn_xi+"");
        tv_bei_Mytotal.setText(direction_mIn_bei+"");
    }

    @OnClick({R.id.iv_direction_dong, R.id.iv_direction_nan, R.id.iv_direction_xi, R.id.iv_direction_bei, R.id.main_amount_10, R.id.main_amount_100, R.id.main_amount_50, R.id.main_amount_500, R.id.tv_bottom_recharge, R.id.tv_bottom_exchange, R.id.tv_bottom_presented, R.id.tv_bottom_out})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_direction_dong:
                pressDirection("dong");
                break;
            case R.id.iv_direction_nan:
                pressDirection("nan");
                break;
            case R.id.iv_direction_xi:
                pressDirection("xi");
                break;
            case R.id.iv_direction_bei:
                pressDirection("bei");
                break;
            case R.id.main_amount_10:
                selectAmount(10);
                break;
            case R.id.main_amount_100:
                selectAmount(100);
                break;
            case R.id.main_amount_50:
                selectAmount(50);
                break;
            case R.id.main_amount_500:
                selectAmount(500);
                break;
            case R.id.tv_bottom_recharge:
                break;
            case R.id.tv_bottom_exchange:
                break;
            case R.id.tv_bottom_presented:
                break;
            case R.id.tv_bottom_out:
                BmobUser.logOut(this);   //清除缓存用户对象
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                break;
        }
    }

    /** 按下方位按钮 并更新用户金币(这里需要检查用户金币数)**/

    private void pressDirection(String direction) {
        switch (direction) {
            case "dong":
                if (checkSystemMoney(direction_mIn_dong)) {
                    break;
                }
                currentGameBean.setTotalIn_dong(currentGameBean.getTotalIn_dong() + currentSelectAmount);//总额增加
                direction_mIn_dong += currentSelectAmount;
                tv_dong_Mytotal.setText( direction_mIn_dong+"" );
                break;
            case "nan":
                if (checkSystemMoney(direction_mIn_nan)) {
                    break;
                }
                currentGameBean.setTotalIn_nan(currentGameBean.getTotalIn_nan() + currentSelectAmount);
                direction_mIn_nan += currentSelectAmount;
                tv_nan_Mytotal.setText( direction_mIn_nan+"" );
                break;
            case "xi":
                if (checkSystemMoney(direction_mIn_xi)) {
                    break;
                }
                currentGameBean.setTotalIn_xi(currentGameBean.getTotalIn_xi() + currentSelectAmount);
                direction_mIn_xi += currentSelectAmount;
                tv_xi_Mytotal.setText( direction_mIn_xi +"");
                break;
            case "bei":
                if (checkSystemMoney(direction_mIn_bei)) {
                    break;
                }
                currentGameBean.setTotalIn_bei(currentGameBean.getTotalIn_bei() + currentSelectAmount);
                direction_mIn_bei += currentSelectAmount;
                tv_bei_Mytotal.setText( direction_mIn_bei+"");
                break;
        }

        //上传数据 total 实时更新GameBean
        currentGameBean.update(this, new UpdateListener() {
            @Override
            public void onSuccess() {
//                Toast.makeText(DisplayCCActivity.this, "currentGameBean上传到服务器成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i, String s) {
                Toast.makeText(DisplayCCActivity.this, "currentGameBean上传到服务器失败" + s, Toast.LENGTH_SHORT).show();

            }
        });

    }
    /** 所选择的一次下注金额**/
    private void selectAmount(int i) {
        currentSelectAmount = i;
    }

    /** 判断 下注是否符合系统规则 里面 还有多少money 是否超标**/
    // 荷包数量减少 总额增多,然后上传
    //如果监听到 gamebean 的 finish字段为true,就判断数据,然后同步数据;
    public Boolean checkSystemMoney(int direction_TotalMonty) {
        //最多:2000+( 4个区域总和-2000)/4
        int fourDirecBySubtionMoney = (currentGameBean.getTotal()) - 2000;//todo  这里因该是总金额 不是自己投的
        int topMonty = 2000 + (fourDirecBySubtionMoney > 0 ? fourDirecBySubtionMoney / 4 : 0);//目前支持最大的下注
        int tempMyTop = direction_TotalMonty + currentSelectAmount;//选择之后 预计会达到的下注
        if ((tempMyTop) > topMonty) {
            return true;//超标了
        } else {
            return checkMyMoney();//false为没有超标
        }
    }
    /** 检查 还有多少money 是否超标 如何没有就更新界面**/
    private Boolean checkMyMoney() {
        if (currentUser.getMoney() >= currentSelectAmount) {
            currentUser.setMoney(currentUser.getMoney() - currentSelectAmount);    // 荷包数量减少 总额增多 这里是数量减少
            updateView();
            return false;
        } else {
            Toast.makeText(this, "余额不足", Toast.LENGTH_SHORT).show();
            return true; // FIXME: 16/5/13
//            return false;
        }
    }


/** 结算时弹出的对话框**/
    public void dialogShow(int answer, String lotteryResult) {
        dialogBuilder = NiftyDialogBuilder.getInstance(this);
        dialogBuilder
                .withTitle("开奖")                                  //.withTitle(null)  no title
                .withTitleColor("#FFFFFF")                                  //def
                .withDividerColor("#11000000")                              //def
                .withMessage("开奖结果: " + lotteryResult)                     //.withMessage(null)  no Msg
                .withMessageColor("#FFFFFFFF")                              //def  | withMessageColor(int resid)
                .withDialogColor("#A935B5")                               //def  | withDialogColor(int resid)                               //def
//                .withIcon(getResources().getDrawable(R.drawable.icon))
                .isCancelableOnTouchOutside(true)                           //def    | isCancelable(true)
                .withDuration(700)                                          //def
                .withEffect(Effectstype.Fliph)                                         //def Effectstype.Slidetop
                .withButton1Text("确定")                                      //def gone
                .withButton2Text("取消")                                  //def gone
                .setCustomView(R.layout.custom_view, this)         //.setCustomView(View or ResId,context)
                .setButton1Click(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogBuilder.dismiss();
                    }
                })
                .setButton2Click(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogBuilder.dismiss();
                    }
                })
                .show();

    }

}
