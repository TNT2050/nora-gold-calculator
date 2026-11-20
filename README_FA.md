# پروژه اندروید ماشین حساب طلای نورا (NORA GOLD)

این پروژه یک اپ اندرویدی ساده (WebView wrapper) است که صفحه ماشین‌حساب HTML را به شکل اپ مستقل و آفلاین اجرا می‌کند.

## ساخت APK در Android Studio
1. Android Studio را نصب و اجرا کنید.
2. گزینه **Open** را بزنید و پوشه `NoraGoldAndroid` را انتخاب کنید.
3. منتظر Gradle Sync بمانید و در صورت درخواست، SDK Platform 35 و Build Tools را نصب کنید.
4. از منوی **Build → Build APK(s)** خروجی APK بسازید.
5. مسیر خروجی معمولاً `app/build/outputs/apk/debug/app-debug.apk` است.
6. فایل APK را به گوشی منتقل کرده و نصب کنید. اگر درخواست شد، اجازه نصب از منبع موردنظر را فعال کنید.

## ویژگی‌ها
- اجرای آفلاین
- ذخیره مظنه آخر با localStorage
- رابط فارسی و RTL
- مظنه/شکن، اونس، تبدیل عیار و فاکتور فروش

## شناسه برنامه
`ir.noragold.calculator`
