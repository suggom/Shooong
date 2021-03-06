package com.kosmo.shooong.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.kosmo.shooong.R;
import com.kosmo.shooong.VolleySingleton;
import com.kosmo.shooong.item.FragmentCourseItem;

import java.util.List;

public class Fragment3CourseAdapter extends BaseAdapter {
    //생성자를 통해서 초기화 할 멤버 변수들]

    //리스트뷰가 실행되는 컨텍스트
    private Context context;
    //리스트뷰에 뿌릴 데이타
    private List<FragmentCourseItem> items;
    //레이아웃 리소스 아이디(선택사항)
    private int layoutResId;

    private Bundle bundle;

    //2]생성자 정의:생성자로 Context와 리스트뷰에 뿌려줄 데이타를 받는다.
    //             리소스 레이아웃 아이디(int)는 선택사항
    //인자생성자2]컨텍스트와 레이아웃 리소스 아이디 그리고 데이타
    public Fragment3CourseAdapter(Context context, int layoutResId, List<FragmentCourseItem> items) {
        this.context = context;
        this.items = items;
        this.layoutResId = layoutResId;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView=View.inflate(context,layoutResId,null);
        }
        //리스트 뷰에서 하나의 아이템항목을 구성하는 각 위젯의 데이타 설정]
        //텍스트뷰 위젯 얻고 데이터 설정]
        ((TextView)convertView.findViewById(R.id.itemCate)).setText(items.get(position).getCourseCateName());
        ((TextView)convertView.findViewById(R.id.itemName)).setText(items.get(position).getCourseName());
        ((TextView)convertView.findViewById(R.id.itemLength)).setText(items.get(position).getCourseLength());
        ((TextView)convertView.findViewById(R.id.itemDuration)).setText(items.get(position).getCourseTime());
        ((TextView)convertView.findViewById(R.id.itemDate)).setText(items.get(position).getCourseRegiDate());
        ((TextView)convertView.findViewById(R.id.itemCourseNo)).setText(items.get(position).getCourseId());
        if(position % 2==0)
            convertView.setBackgroundColor(0xFFFFFFFF);
        else
            convertView.setBackgroundColor(0xFFC0C0C0);
        //아이템 이벤트 처리]
        //아이템 항목(이미지뷰하나,텍스트뷰 하나구성) 클릭시
        final int index = position;
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context,items.get(index).getCourseName(),Toast.LENGTH_SHORT).show();
            }
        });
        /*
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                String courseId = items.get(index).getCourseId();
                bundle.putString("courseId",courseId);
                return false;
            }
        });
        */
        return convertView;
    }
}
