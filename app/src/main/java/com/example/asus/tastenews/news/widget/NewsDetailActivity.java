package com.example.asus.tastenews.news.widget;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.asus.tastenews.R;
import com.example.asus.tastenews.beans.CommentBean;
import com.example.asus.tastenews.beans.NewsBean;
import com.example.asus.tastenews.beans.UserBean;
import com.example.asus.tastenews.news.presenter.NewsDetailPresenter;
import com.example.asus.tastenews.news.presenter.NewsDetailPresenterImpl;
import com.example.asus.tastenews.news.view.NewsDetailView;
import com.example.asus.tastenews.ui.BarrageView;
import com.example.asus.tastenews.utils.ImageLoaderUtils;
import com.example.asus.tastenews.utils.LogUtils;
import com.example.asus.tastenews.utils.ToolUtils;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.GetListener;
import me.imid.swipebacklayout.lib.SwipeBackLayout;
import me.imid.swipebacklayout.lib.app.SwipeBackActivity;
import mehdi.sakout.fancybuttons.FancyButton;

/**
 * Created by ASUS on 2016/5/28.
 */
public class NewsDetailActivity extends SwipeBackActivity implements NewsDetailView {

    private NewsBean mNews;
    private HtmlTextView mTVNewsContent;
    private NewsDetailPresenter mNewsDetailPresenter;
    private ProgressBar mProgressBar;
    private SwipeBackLayout mSwipeBackLayout;
    private NestedScrollView mNestedScollView;

    private Button mCommentButton;
    private RecyclerView mCommentRV;
    private EditText mCommentET;
    private CommentAdapter mCommentAdapter;

    private List<CommentBean>mCommentBeanList = new ArrayList<>();

    private static final int DELAY_TIME = 2000;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        mProgressBar = (ProgressBar)findViewById(R.id.progress);
        mTVNewsContent = (HtmlTextView)findViewById(R.id.htNewsContent);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//返回键
        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                onBackPressed();
            }
        });

        mSwipeBackLayout = getSwipeBackLayout();
        mSwipeBackLayout.setEdgeSize(ToolUtils.getWidthInPx(this));
        mSwipeBackLayout.setEdgeTrackingEnabled(SwipeBackLayout.EDGE_LEFT);
        mNestedScollView = (NestedScrollView)findViewById(R.id.detail_nsv);

        mCommentButton = (Button)findViewById(R.id.comment_bt);
        mCommentET = (EditText)findViewById(R.id.comment_et);
        mCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserBean user = BmobUser.getCurrentUser(NewsDetailActivity.this,UserBean.class);
                String comment = mCommentET.getText() == null ? null : mCommentET.getText().toString();
                mNewsDetailPresenter.sendCommentToNews(user,comment);
            }
        });

        mNews = (NewsBean)getIntent().getSerializableExtra("news");

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout)findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(mNews.getTitle());

        ImageLoaderUtils.display(getApplicationContext(),(ImageView)findViewById(R.id.ivImage),mNews.getImgsrc());

        mNewsDetailPresenter = new NewsDetailPresenterImpl(getApplication(),this);
        mNewsDetailPresenter.loadNewsDetail(mNews.getDocid());
    }

    @Override
    public void showNewsDetailContent(String newsDetailContent, List<CommentBean>commentBeanList){
        mTVNewsContent.setHtmlFromString(newsDetailContent,new HtmlTextView.LocalImageGetter());

        LogUtils.d("opopp","commentBeans are " + commentBeanList);
        mCommentBeanList = commentBeanList;
        mCommentRV = (RecyclerView)findViewById(R.id.rvComment);
        mCommentRV.setLayoutManager(new LinearLayoutManager(this));
        mCommentAdapter = new CommentAdapter();
        mCommentRV.setAdapter(mCommentAdapter);
        setBarrageView();
    }

    private void setBarrageView(){
        int size = mCommentBeanList.size();
        if(size == 0)
            return;

        final List<CommentBean> copyCommentBeanList = new ArrayList<>();
        copyCommentBeanList.addAll(mCommentBeanList);

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final BarrageView bv = new BarrageView(NewsDetailActivity.this);
                bv.setText(copyCommentBeanList.get(0).getContent());
                LogUtils.d("PPPP","text is " + bv.getText());
                addContentView
                        (bv, new ViewGroup.LayoutParams
                                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                copyCommentBeanList.remove(0);
                if(copyCommentBeanList.size() > 0)
                    handler.postDelayed(this,DELAY_TIME);
            }
        };

        handler.post(runnable);
    }

    @Override
    public void showProgress(){
        mCommentButton.setVisibility(View.GONE);
        mCommentET.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress(){
        mCommentButton.setVisibility(View.VISIBLE);
        mCommentET.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void successComment(String comment) {
        mCommentET.setText("");
        UserBean bmobUser = UserBean.getCurrentUser(this,UserBean.class);
        CommentBean commentBean = new CommentBean();
        commentBean.setComment_people(bmobUser);
        commentBean.setContent(comment);
        mCommentBeanList.add(0,commentBean);
        mCommentAdapter.notifyDataSetChanged();
    }

    @Override
    public void failureComment() {
    }

    class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder>{

        @Override
        public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            CommentViewHolder holder = new CommentViewHolder(LayoutInflater
                    .from(NewsDetailActivity.this)
                    .inflate(R.layout.item_comment,parent,false));
            return holder;
        }

        @Override
        public void onBindViewHolder(final CommentViewHolder holder, int position) {
            CommentBean bean = mCommentBeanList.get(position);
            String objectid = bean.getComment_people().getObjectId();
            BmobQuery<UserBean> query = new BmobQuery<UserBean>();
            query.getObject(NewsDetailActivity.this, objectid,new GetListener<UserBean>() {
                @Override
                public void onSuccess(UserBean user) {
                    holder.mNameTV.setText(user.getUsername());
                }

                @Override
                public void onFailure(int i, String s) {
                    holder.mNameTV.setText("某用户");
                }
            });
            holder.mCommentTV.setText(bean.getContent());

        }

        @Override
        public int getItemCount() {
            return mCommentBeanList.size();
        }

        class CommentViewHolder extends RecyclerView.ViewHolder{
            ImageView mHeadIV;
            TextView mNameTV;
            TextView mCommentTV;
            public CommentViewHolder(View view){
                super(view);
                mHeadIV = (ImageView)view.findViewById(R.id.ivNews);
                mNameTV = (TextView)view.findViewById(R.id.tvName);
                mCommentTV = (TextView)view.findViewById(R.id.tvComment);
            }
        }
    }
}
