package com.example.mapslocalization.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.mapslocalization.R;
import com.example.mapslocalization.models.ClusterMarker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

public class ClusterManagerRenderer extends DefaultClusterRenderer<ClusterMarker> {

    private final IconGenerator iconGenerator;
    private ImageView imageView;
    private final int markerWidth;
    private final int markerHeight;

    public ClusterManagerRenderer(Context context, GoogleMap map, ClusterManager<ClusterMarker> clusterManager) {
        super(context, map, clusterManager);
        //this.iconGenerator = iconGenerator;
        this.imageView = imageView;
        //this.markerWidth = markerWidth;
        //this.markerHeight = markerHeight;

        iconGenerator = new IconGenerator(context.getApplicationContext());
        imageView = new ImageView(context.getApplicationContext());
        markerWidth = (int) context.getResources().getDimension(R.dimen.cardview_compat_inset_shadow);
        markerHeight = (int) context.getResources().getDimension(R.dimen.cardview_compat_inset_shadow);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(markerWidth, markerHeight));
        int padding = (int) context.getResources().getDimension(R.dimen.cardview_compat_inset_shadow);
        imageView.setPadding(padding, padding, padding, padding);
        iconGenerator.setContentView(imageView);

    }

    @Override
    protected void onBeforeClusterItemRendered(ClusterMarker item, MarkerOptions markerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions);

        imageView.setImageResource(item.getIconPicture());
        Bitmap icon = iconGenerator.makeIcon();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(item.getTitle());
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<ClusterMarker> cluster) {
        //if a cluster is required
        //return super.shouldRenderAsCluster(cluster);
        return false;
    }
}
