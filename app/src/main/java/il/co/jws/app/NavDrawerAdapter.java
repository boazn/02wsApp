package il.co.jws.app;

import android.app.Activity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
/**
 * Created by boaz on 02/09/2017.
 */

public class NavDrawerAdapter extends ArrayAdapter<NavDrawerItem>
{
    private final Context context;
    private final int layoutResourceId;
    private NavDrawerItem data[] = null;

    public NavDrawerAdapter(Context context, int layoutResourceId, NavDrawerItem [] data)
    {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();

        View v = inflater.inflate(layoutResourceId, parent, false);

        ImageView imageView = (ImageView) v.findViewById(R.id.drawer_item_icons);
        TextView textView = (TextView) v.findViewById(R.id.drawer_item_labels);

        NavDrawerItem choice = data[position];

        imageView.setImageResource(choice.icon);
        textView.setText(choice.name);

        return v;
    }
}


