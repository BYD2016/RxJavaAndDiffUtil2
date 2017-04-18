package se.hellsoft.diffutilandrxjava;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.computation;

public final class MainActivity extends AppCompatActivity {
    private MyAdapter adapter;
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.listOfThings);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);

        List<Thing> emptyList = Collections.emptyList();
        adapter.setThings(emptyList);

        Pair<List<Thing>, DiffUtil.DiffResult> initialPair = Pair.create(emptyList, null);

        disposable = ThingRepository
                .latestThings(2, TimeUnit.SECONDS)
                .scan(initialPair, (pair, news) -> {
                    MyDiffCallback callback = new MyDiffCallback(pair.first, news);
                    DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback);
                    return Pair.create(news, result);
                })
                .skip(1)
                .subscribeOn(computation())
                .observeOn(mainThread())
                .subscribe(listDiffResultPair -> {
                    adapter.setThings(listDiffResultPair.first);
                    listDiffResultPair.second.dispatchUpdatesTo(adapter);
                });
    }

    @Override
    protected void onStop() {
        disposable.dispose();
        super.onStop();
    }

    private static final class MyAdapter extends RecyclerView.Adapter<ThingViewHolder> {
        private List<Thing> things = new ArrayList<>(); // Start with empty list

        @Override
        public ThingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.thing_item, parent, false);
            return new ThingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ThingViewHolder holder, int position) {
            Thing thing = things.get(position);
            holder.bind(thing);
        }

        @Override
        public int getItemCount() {
            return things.size();
        }

        void setThings(List<Thing> things) {
            this.things = things;
        }
    }

    private static final class ThingViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;

        ThingViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.text);
        }

        void bind(Thing thing) {
            itemView.setBackgroundColor(thing.getColor());
            textView.setText(thing.getText());
        }
    }

    private static class MyDiffCallback extends DiffUtil.Callback {
        private List<Thing> mOlds;
        private List<Thing> mNews;

        MyDiffCallback(List<Thing> olds, List<Thing> news) {
            this.mOlds = olds;
            this.mNews = news;
        }

        @Override
        public int getOldListSize() {
            return mOlds.size();
        }

        @Override
        public int getNewListSize() {
            return mNews.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Thing oldItem = mOlds.get(oldItemPosition);
            Thing newItem = mNews.get(newItemPosition);
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Thing oldItem = mOlds.get(oldItemPosition);
            Thing newItem = mNews.get(newItemPosition);
            return oldItem.equals(newItem);
        }
    }
}
