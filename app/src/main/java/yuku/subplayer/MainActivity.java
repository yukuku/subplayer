package yuku.subplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import kotlin.io.TextStreamsKt;
import yuku.subplayer.parser.SrtInput;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {
	private static final int REQCODE_open = 1;
	SrtInput sub;
	long beginTime;

	TextView tPrevs;
	TextView tCurrent;
	TextView tNexts;

	Handler h = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tPrevs = (TextView) findViewById(R.id.tPrevs);
		tCurrent = (TextView) findViewById(R.id.tCurrent);
		tNexts = (TextView) findViewById(R.id.tNexts);
		beginTime = System.currentTimeMillis();

		h.post(new Runnable() {
			@Override
			public void run() {
				if (isFinishing()) return;

				display();
				h.postDelayed(this, 50);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == R.id.menuOpen) {
			final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			intent.setType("*/*");
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			startActivityForResult(intent, REQCODE_open);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQCODE_open && resultCode == RESULT_OK && data != null) {
			try {
				final ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(data.getData(), "r");
				if (pfd != null) {
					final String body = TextStreamsKt.readText(new InputStreamReader(new FileInputStream(pfd.getFileDescriptor()), Charset.forName("utf-8")));
					sub = new SrtInput(body);
					sub.parse();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	void display() {
		if (sub == null) return;
		final long time = System.currentTimeMillis() - beginTime;
		final int index = sub.findIndex(time);
		if (index == -1) return;

		if (index > 0) {
			tPrevs.setText(sub.events.get(index - 1).getText());
		} else {
			tPrevs.setText("");
		}
		tCurrent.setText(sub.events.get(index).getText());
		if (index < sub.events.size() - 1) {
			tNexts.setText(sub.events.get(index + 1).getText());
		} else {
			tNexts.setText("");
		}
	}

	public void prev_click(View v) {
		if (sub == null) return;
		final long time = System.currentTimeMillis() - beginTime;
		final int index = sub.findIndex(time);
		if (index == -1) return;

		if (index > 0) {
			final int newIndex = index - 1;
			beginTime = System.currentTimeMillis() - sub.events.get(newIndex).getStartTime();
			display();
		}
	}

	public void next_click(View v) {
		if (sub == null) return;
		final long time = System.currentTimeMillis() - beginTime;
		final int index = sub.findIndex(time);
		if (index == -1) return;

		if (index < sub.events.size() - 1) {
			final int newIndex = index + 1;
			beginTime = System.currentTimeMillis() - sub.events.get(newIndex).getStartTime();
			display();
		}
	}

	void showError(final Exception e) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new MaterialDialog.Builder(MainActivity.this)
					.content(e.getMessage())
					.positiveText("OK")
					.show();
			}
		});
	}
}
