package fr.ynryo.spotted.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class SquareLinearLayout extends LinearLayout {

    public SquareLinearLayout(Context context) {
        super(context);
    }

    public SquareLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Appelé par le layout manager pour déterminer la taille de la vue avant l'affichage
     * Modifie la largeur pour qu'elle soit égale à la hauteur
     *
     * @param widthMeasureSpec  Mesure de la largeur
     * @param heightMeasureSpec Mesure de la hauteur
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = getMeasuredHeight();
        int width = Math.max(getMeasuredWidth(), height);
        setMeasuredDimension(width, height);
    }
}
