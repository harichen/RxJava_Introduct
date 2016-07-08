#RxJava分享
### RxJava 是什么
一个词：**异步** 

RxJava 在 GitHub 主页上的自我介绍是 "a library for composing asynchronous and event-based programs using observable sequences for the Java VM"（一个在 Java VM 上使用可观测的序列来组成异步的、基于事件的程序的库）。

简而言之就是一个实现异步操作的库。

### RxJava 好在哪

同样是做异步，为什么用它，而不用现成的AyncTask/Handler 等？

一个词：**函数式编程**

异步操作很关键的一点是程序的简洁性，因为在调度过程比较复杂的情况下，异步代码经常会既难写也难被读懂。 Android 创造的 AsyncTask 和Handler ，其实都是为了让异步代码更加简洁。RxJava 的优势也是简洁，但它的简洁的与众不同之处在于，随着程序逻辑变得越来越复杂，它依然能够保持简洁。

这是因为Rxjava的语法特性，函数式编程。

函数式编程更多地表达了业务逻辑的意图，而不是它的实现机制。其核心是：在思考问题时，使用不可变值和函数，函数对一个值进行处理，映射成另一个值。

举一个简单的栗子：
Collections.reverse(List<?> list); 我们常用的容器倒序方法就是一个简单的函数。它对一个list进行处理，最后输出一个倒叙过后的list。我们只关心输出的结果，而不必关心实现的过程。

随着程序逻辑变得越来越复杂，使用RxJava的优势就凸显了。

假设有这样一个需求：界面上有一个自定义的视图 imageCollectorView ，它的作用是显示多张图片，并能使用 addImage(Bitmap) 方法来任意增加显示的图片。现在需要程序将一个给出的目录数组 File[] folders 中每个目录下的 png 图片都加载出来并显示在 imageCollectorView 中。需要注意的是，由于读取图片的这一过程较为耗时，需要放在后台执行，而图片的显示则必须在 UI 线程执行。常用的实现方式有多种，我这里贴出其中一种：

	new Thread() {
    @Override
    public void run() {
        super.run();
        for (File folder : folders) {
            File[] files = folder.listFiles();
            for (File file : files) {
                if (file.getName().endsWith(".png")) {
                    final Bitmap bitmap = getBitmapFromFile(file);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageCollectorView.addImage(bitmap);
                        }
                    });
                }
            }
        }
    }
	}.start();
	
而如果使用 RxJava ，实现方式是这样的：

	Observable.from(folders)
    .flatMap(new Func1<File, Observable<File>>() {
        @Override
        public Observable<File> call(File file) {
            return Observable.from(file.listFiles());
        }
    })
    .filter(new Func1<File, Boolean>() {
        @Override
        public Boolean call(File file) {
            return file.getName().endsWith(".png");
        }
    })
    .map(new Func1<File, Bitmap>() {
        @Override
        public Bitmap call(File file) {
            return getBitmapFromFile(file);
        }
    })
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Action1<Bitmap>() {
        @Override
        public void call(Bitmap bitmap) {
            imageCollectorView.addImage(bitmap);
        }
    });

RxJava 的这个实现，**是一条从上到下的链式调用，没有任何嵌套，这在逻辑的简洁性上是具有优势的。**当需求变得复杂时，这种优势将更加明显（试想如果还要求只选取前 10 张图片，常规方式要怎么办？如果有更多这样那样的要求呢？再试想，在这一大堆需求实现完两个月之后需要改功能，当你翻回这里看到自己当初写下的那一片迷之缩进，你能保证自己将迅速看懂，而不是对着代码重新捋一遍思路？），移动开发中有很多这样的场景。

另外，如果配合Lambda表达式会变得更加的简洁。

### RxJava 怎么用
#### 1. 概念：扩展的观察者模式

RxJava 的异步实现，是通过一种扩展的观察者模式来实现的。

观察者模式

先简述一下观察者模式，已经熟悉的可以跳过这一段。

观察者模式面向的需求是：A 对象（观察者）对 B 对象（被观察者）的某种变化高度敏感，需要在 B 变化的一瞬间做出反应。举个例子，新闻里喜闻乐见的警察抓小偷，警察需要在小偷伸手作案的时候实施抓捕。在这个例子里，警察是观察者，小偷是被观察者，警察需要时刻盯着小偷的一举一动，才能保证不会漏过任何瞬间。程序的观察者模式和这种真正的『观察』略有不同，观察者不需要时刻盯着被观察者（例如 A 不需要每过 2ms 就检查一次 B 的状态），而是采用注册(Register)或者称为订阅(Subscribe)的方式，告诉被观察者：我需要你的某某状态，你要在它变化的时候通知我。 Android 开发中一个比较典型的例子是点击监听器 OnClickListener 。对设置 OnClickListener 来说， View 是被观察者， OnClickListener 是观察者，二者通过 setOnClickListener() 方法达成订阅关系。订阅之后用户点击按钮的瞬间，Android Framework 就会将点击事件发送给已经注册的 OnClickListener 。采取这样被动的观察方式，既省去了反复检索状态的资源消耗，也能够得到最高的反馈速度。当然，这也得益于我们可以随意定制自己程序中的观察者和被观察者，而警察叔叔明显无法要求小偷『你在作案的时候务必通知我』。

OnClickListener 的模式大致如下图：

![](http://ww3.sinaimg.cn/large/006tNc79jw1f5mgnudwztj30fz03rq33)

如图所示，通过 setOnClickListener() 方法，Button 持有 OnClickListener 的引用（这一过程没有在图上画出）；当用户点击时，Button 自动调用 OnClickListener 的 onClick() 方法。另外，如果把这张图中的概念抽象出来（Button -> 被观察者、OnClickListener -> 观察者、setOnClickListener() -> 订阅，onClick() -> 事件），就由专用的观察者模式（例如只用于监听控件点击）转变成了通用的观察者模式。如下图：

![](http://ww3.sinaimg.cn/large/006tNc79jw1f5mgrkgjndj30ga03pq33)

而 RxJava 作为一个工具库，使用的就是通用形式的观察者模式。

##### RxJava 的观察者模式

RxJava 有四个基本概念：**Observable** (可观察者，即被观察者)、 **Observer** (观察者)、 **subscribe** (订阅)、事件。**Observable** 和 **Observer** 通过 **subscribe**() 方法实现订阅关系，从而 **Observable** 可以在需要的时候发出事件来通知 **Observer**。

与传统观察者模式不同， RxJava 的事件回调方法除了普通事件 onNext() （相当于 onClick() / onEvent()）之外，还定义了两个特殊的事件：onCompleted() 和 onError()。

onCompleted(): 事件队列完结。RxJava 不仅把每个事件单独处理，还会把它们看做一个队列。RxJava 规定，当不会再有新的 onNext() 发出时，需要触发 onCompleted() 方法作为标志。
onError(): 事件队列异常。在事件处理过程中出异常时，onError() 会被触发，同时队列自动终止，不允许再有事件发出。
在一个正确运行的事件序列中, onCompleted() 和 onError() 有且只有一个，并且是事件序列中的最后一个。需要注意的是，onCompleted() 和 onError() 二者也是互斥的，即在队列中调用了其中一个，就不应该再调用另一个。

RxJava 的观察者模式大致如下图：

![](http://ww3.sinaimg.cn/large/006tNc79jw1f5mh1t32cfj30gn04qweq)

#### 2. 基本实现
基于以上的概念， RxJava 的基本实现主要有三点：

1) 创建 Observer

Observer 即观察者，它决定事件触发的时候将有怎样的行为。 RxJava 中的 Observer 接口的实现方式：

	Observer<String> observer = new Observer<String>() {
    @Override
    public void onNext(String s) {
        Log.d(tag, "Item: " + s);
    }

    @Override
    public void onCompleted() {
        Log.d(tag, "Completed!");
    }

    @Override
    public void onError(Throwable e) {
        Log.d(tag, "Error!");
    }
	};

2) 创建 Observable

Observable 即被观察者，它决定什么时候触发事件以及触发怎样的事件。 RxJava 使用 create() 方法来创建一个 Observable ，并为它定义事件触发规则：

	Observable observable = Observable.create(new Observable.OnSubscribe<String>() {
    @Override
    public void call(Subscriber<? super String> subscriber) {
        subscriber.onNext("Hello");
        subscriber.onNext("Hi");
        subscriber.onNext("Aloha");
        subscriber.onCompleted();
    }
	});
可以看到，这里传入了一个 OnSubscribe 对象作为参数。OnSubscribe 会被存储在返回的 Observable 对象中，它的作用相当于一个计划表，当 Observable 被订阅的时候，OnSubscribe 的 call() 方法会自动被调用，事件序列就会依照设定依次触发（对于上面的代码，就是观察者Subscriber 将会被调用三次 onNext() 和一次 onCompleted()）。这样，由被观察者调用了观察者的回调方法，就实现了由被观察者向观察者的事件传递，即观察者模式。

***create() ***方法是 RxJava 最基本的创造事件序列的方法。基于这个方法， RxJava 还提供了一些方法用来快捷创建事件队列，这里就不一一举例了。

3) Subscribe (订阅)

创建了 Observable 和 Observer 之后，再用 subscribe() 方法将它们联结起来，整条链子就可以工作了。代码形式很简单：

	observable.subscribe(observer);
	// 或者：
	observable.subscribe(subscriber);
	
Observable.subscribe(Subscriber) 的内部实现是这样的（仅核心代码）：

	// 注意：这不是 subscribe() 的源码，而是将源码中与性能、兼容性、扩展性有关的代码剔除后的核心代码。
	// 如果需要看源码，可以去 RxJava 的 GitHub 仓库下载。
	public Subscription subscribe(Subscriber subscriber) {
    subscriber.onStart();
    onSubscribe.call(subscriber);
    return subscriber;
	}


可以看到，subscriber() 做了3件事：

1.调用 Subscriber.onStart() 。这个方法在前面已经介绍过，是一个可选的准备方法。

2.调用 Observable 中的 OnSubscribe.call(Subscriber) 。在这里，事件发送的逻辑开始运行。从这也可以看出，在 RxJava 中， Observable 并不是在创建的时候就立即开始发送事件，而是在它被订阅的时候，即当 subscribe() 方法执行的时候。

3.将传入的 Subscriber 作为 Subscription 返回。这是为了方便 unsubscribe().

整个过程中对象间的关系如下图：

![](http://ww3.sinaimg.cn/large/006tNc79jw1f5mjkp8asnj30lk0a8js3)

为了把原理用更清晰的方式表述出来，本文中挑选的都是功能尽可能简单的例子.
由指定的一个 drawable 文件 id drawableRes 取得图片，并显示在 ImageView 中，并在出现异常的时候打印 Toast 报错：

	int drawableRes = ...;
	ImageView imageView = ...;
	Observable.create(new OnSubscribe<Drawable>() {
    @Override
    public void call(Subscriber<? super Drawable> subscriber) {
        Drawable drawable = getTheme().getDrawable(drawableRes));
        subscriber.onNext(drawable);
        subscriber.onCompleted();
    }
	}).subscribe(new Observer<Drawable>() {
    @Override
    public void onNext(Drawable drawable) {
        imageView.setImageDrawable(drawable);
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable e) {
        Toast.makeText(activity, "Error!", Toast.LENGTH_SHORT).show();
    }
	});

**注：正如前面所提到的，Observer 和 Subscriber 具有相同的角色，而且 Observer 在 subscribe() 过程中最终会被转换成 Subscriber 对象，因此，从这里开始，后面的描述我将用 Subscriber 来代替 Observer ，这样更加严谨。**

在 RxJava 的默认规则中，事件的发出和消费都是在同一个线程的。也就是说，如果只用上面的方法，实现出来的只是一个同步的观察者模式。观察者模式本身的目的就是『后台处理，前台回调』的异步机制，因此异步对于 RxJava 是至关重要的。而要实现异步，则需要用到 RxJava 的另一个概念： Scheduler 。

#### 3. 线程控制 —— Scheduler
在不指定线程的情况下， RxJava 遵循的是线程不变的原则，即：在哪个线程调用 **subscribe()**，就在哪个线程生产事件；在哪个线程生产事件，就在哪个线程消费事件。如果需要切换线程，就需要用到 **Scheduler** （调度器）。

1) Scheduler 的 API (一)

在RxJava 中，Scheduler ——调度器，相当于线程控制器，RxJava 通过它来指定每一段代码应该运行在什么样的线程。RxJava 已经内置了几个 Scheduler ，它们已经适合大多数的使用场景：

Schedulers.immediate(): 直接在当前线程运行，相当于不指定线程。这是默认的 Scheduler。
Schedulers.newThread(): 总是启用新线程，并在新线程执行操作。
Schedulers.io(): I/O 操作（读写文件、读写数据库、网络信息交互等）所使用的 Scheduler。行为模式和 newThread() 差不多，区别在于 io() 的内部实现是是用一个无数量上限的线程池，可以重用空闲的线程，因此多数情况下 io() 比 newThread() 更有效率。不要把计算工作放在 io() 中，可以避免创建不必要的线程。
Schedulers.computation(): 计算所使用的 Scheduler。这个计算指的是 CPU 密集型计算，即不会被 I/O 等操作限制性能的操作，例如图形的计算。这个 Scheduler 使用的固定的线程池，大小为 CPU 核数。不要把 I/O 操作放在 computation() 中，否则 I/O 操作的等待时间会浪费 CPU。
另外， Android 还有一个专用的 AndroidSchedulers.mainThread()，它指定的操作将在 Android 主线程运行。
有了这几个 Scheduler ，就可以使用 subscribeOn() 和 observeOn() 两个方法来对线程进行控制了。 * subscribeOn(): 指定 subscribe() 所发生的线程，即 Observable.OnSubscribe 被激活时所处的线程。或者叫做事件产生的线程。 * observeOn(): 指定 Subscriber 所运行在的线程。或者叫做事件消费的线程。

文字叙述总归难理解，上代码：

	Observable.just(1, 2, 3, 4)
              .subscribeOn(Schedulers.io()) // 指定 subscribe() 发生在 IO 线程
              .observeOn(AndroidSchedulers.mainThread()) // 指定 Subscriber 的回调发生在主线程
              .subscribe(new Action1<Integer>() {
                  @Override
                  public void call(Integer number) {
                      Log.d(tag, "number:" + number);
                  }
              });
上面这段代码中，由于 subscribeOn(Schedulers.io()) 的指定，被创建的事件的内容 1、2、3、4 将会在 IO 线程发出；而由于 observeOn(AndroidScheculers.mainThread()) 的指定，因此 subscriber 数字的打印将发生在主线程 。事实上，这种在 subscribe() 之前写上两句 subscribeOn(Scheduler.io()) 和 observeOn(AndroidSchedulers.mainThread()) 的使用方式非常常见，它适用于多数的 『后台线程取数据，主线程显示』的程序策略。

而前面提到的由图片 id 取得图片并显示的例子，如果也加上这两句：

	int drawableRes = ...;
	ImageView imageView = ...;
	Observable.create(new OnSubscribe<Drawable>() {
    @Override
    public void call(Subscriber<? super Drawable> subscriber) {
        Drawable drawable = getTheme().getDrawable(drawableRes));
        subscriber.onNext(drawable);
        subscriber.onCompleted();
    }
	})
	.subscribeOn(Schedulers.io()) // 指定 subscribe() 发生在 IO 线程
	.observeOn(AndroidSchedulers.mainThread()) // 指定 Subscriber 的回调发生在主线程
	.subscribe(new Observer<Drawable>() {
    @Override
    public void onNext(Drawable drawable) {
        imageView.setImageDrawable(drawable);
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable e) {
        Toast.makeText(activity, "Error!", Toast.LENGTH_SHORT).show();
    }
	});
那么，加载图片将会发生在 IO 线程，而设置图片则被设定在了主线程。这就意味着，即使加载图片耗费了几十甚至几百毫秒的时间，也不会造成丝毫界面的卡顿。

#### 4. 变换

RxJava 提供了对事件序列进行变换的支持，这是它的核心功能之一，也是大多数人说『RxJava 真是太好用了』的最大原因。**所谓变换，就是将事件序列中的对象或整个序列进行加工处理，转换成不同的事件或事件序列。也就是说RxJava具备函数式编程的特性，提供了一系列变换函数，这可以大大提高我们的开发效率。**

1) API

首先看一个 map() 的例子：

	Observable.just("images/logo.png") // 输入类型 String
    .map(new Func1<String, Bitmap>() {
        @Override
        public Bitmap call(String filePath) { // 参数类型 String
            return getBitmapFromPath(filePath); // 返回类型 Bitmap
        }
    })
    .subscribe(new Action1<Bitmap>() {
        @Override
        public void call(Bitmap bitmap) { // 参数类型 Bitmap
            showBitmap(bitmap);
        }
    });
可以看到，map() 方法将参数中的 String 对象转换成一个 Bitmap 对象后返回，而在经过 map() 方法后，事件的参数类型也由 String 转为了 Bitmap。这种直接变换对象并返回的，是最常见的也最容易理解的变换。不过 RxJava 的变换远不止这样，它不仅可以针对事件对象，还可以针对整个事件队列，这使得 RxJava 变得非常灵活。我列举几个常用的变换：

map(): 事件对象的直接变换，具体功能上面已经介绍过。它是 RxJava 最常用的变换。 map() 的示意图

![](http://ww3.sinaimg.cn/large/006tNc79jw1f5mkqyxqnlj30hw0ea74u)

至于变换的原理这里就不展开了，有兴趣可以看文章[给 Android 开发者的 RxJava 详解](http://gank.io/post/560e15be2dca930e00da1083)中的相应章节。

#### 5.线程自由切换：Scheduler
除了灵活的变换，RxJava 另一个牛逼的地方，就是线程的自由控制。

1) Scheduler 的 API (二)


前面讲到了，可以利用 subscribeOn() 结合 observeOn() 来实现线程控制，让事件的产生和消费发生在不同的线程。那么，能不能多次切换几次线程呢？

答案是：能。因为 **observeOn()** 指定的是 Subscriber 的线程，而这个 Subscriber 并不是（严格说应该为『不一定是』，但这里不妨理解为『不是』）subscribe() 参数中的 Subscriber ，而是 observeOn() 执行时的当前 Observable 所对应的 Subscriber ，即它的直接下级 Subscriber 。换句话说，observeOn() 指定的是它之后的操作所在的线程。因此如果有多次切换线程的需求，只要在每个想要切换线程的位置调用一次 observeOn() 即可。上代码：

	Observable.just(1, 2, 3, 4) // IO 线程，由 subscribeOn() 指定
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.newThread())
              .map(mapOperator) // 新线程，由 observeOn() 指定
              .observeOn(Schedulers.io())
              .map(mapOperator2) // IO 线程，由 observeOn() 指定
              .observeOn(AndroidSchedulers.mainThread) 
              .subscribe(subscriber);  // Android 主线程，由 observeOn() 指定
如上，通过 observeOn() 的多次调用，程序实现了线程的多次切换。

不过，不同于 observeOn() ， subscribeOn() 的位置放在哪里都可以，但它是只能调用一次的。

如果要调用多次 subscribeOn() 呢？
可以用** doOnSubscribe()**。默认情况下， doOnSubscribe() 执行在 subscribe() 发生的线程；而如果在 doOnSubscribe() 之后有 subscribeOn() 的话，它将执行在离它最近的 subscribeOn() 所指定的线程。

示例代码：

	Observable.create(onSubscribe)
              .subscribeOn(Schedulers.io())
              .doOnSubscribe(new Action0() {
                  @Override
                  public void call() {
                      progressBar.setVisibility(View.VISIBLE); // 需要在主线程执行
                  }
              })
              .subscribeOn(AndroidSchedulers.mainThread()) // 指定主线程
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(subscriber);
如上，在 doOnSubscribe()的后面跟一个 subscribeOn() ，就能指定准备工作的线程了。

### RxJava 的适用场景和使用方式
####1. 与 Retrofit 的结合
Retrofit 除了提供了传统的 Callback 形式的 API，还有 RxJava 版本的 Observable 形式 API。下面我用对比的方式来介绍 Retrofit 的 RxJava 版 API 和传统版本的区别。

以获取一个 User 对象的接口作为例子。使用Retrofit 的传统 API，你可以用这样的方式来定义请求：

	@GET("/user")
	public void getUser(@Query("userId") String userId, Callback<User> callback);
在程序的构建过程中， Retrofit 会把自动把方法实现并生成代码，然后开发者就可以利用下面的方法来获取特定用户并处理响应：

	getUser(userId, new Callback<User>() {
    @Override
    public void success(User user) {
        userView.setUser(user);
    }

    @Override
    public void failure(RetrofitError error) {
        // Error handling
        ...
    }
	};
而使用 RxJava 形式的 API，定义同样的请求是这样的：

	@GET("/user")
	public Observable<User> getUser(@Query("userId") String userId);
使用的时候是这样的：

	getUser(userId)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Observer<User>() {
        @Override
        public void onNext(User user) {
            userView.setUser(user);
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable error) {
            // Error handling
            ...
        }
    });

看到区别了吗？

当 RxJava 形式的时候，Retrofit 把请求封装进 Observable ，在请求结束后调用 onNext() 或在请求失败后调用 onError()。

对比来看， Callback 形式和 Observable 形式长得不太一样，但本质都差不多，而且在细节上 Observable 形式似乎还比 Callback 形式要差点。那 Retrofit 为什么还要提供 RxJava 的支持呢？

因为它好用啊！从这个例子看不出来是因为这只是最简单的情况。而一旦情景复杂起来， Callback 形式马上就会开始让人头疼。比如：

假设这么一种情况：你的程序取到的 User 并不应该直接显示，而是需要先与数据库中的数据进行比对和修正后再显示。使用 Callback 方式大概可以这么写：

	getUser(userId, new Callback<User>() {
    @Override
    public void success(User user) {
        processUser(user); // 尝试修正 User 数据
        userView.setUser(user);
    }

    @Override
    public void failure(RetrofitError error) {
        // Error handling
        ...
    }
	};
有问题吗？

很简便，但不要这样做。为什么？因为这样做会影响性能。数据库的操作很重，一次读写操作花费 10~20ms 是很常见的，这样的耗时很容易造成界面的卡顿。所以通常情况下，如果可以的话一定要避免在主线程中处理数据库。所以为了提升性能，这段代码可以优化一下：

	getUser(userId, new Callback<User>() {
    @Override
    public void success(User user) {
        new Thread() {
            @Override
            public void run() {
                processUser(user); // 尝试修正 User 数据
                runOnUiThread(new Runnable() { // 切回 UI 线程
                    @Override
                    public void run() {
                        userView.setUser(user);
                    }
                });
            }).start();
    }

    @Override
    public void failure(RetrofitError error) {
        // Error handling
        ...
    }
	};
性能问题解决，但……这代码实在是太乱了，迷之缩进啊！杂乱的代码往往不仅仅是美观问题，因为代码越乱往往就越难读懂，而如果项目中充斥着杂乱的代码，无疑会降低代码的可读性，造成团队开发效率的降低和出错率的升高。

这时候，如果用 RxJava 的形式，就好办多了。 RxJava 形式的代码是这样的：

	getUser(userId)
    .doOnNext(new Action1<User>() {
        @Override
        public void call(User user) {
            processUser(user);
        })
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new Observer<User>() {
        @Override
        public void onNext(User user) {
            userView.setUser(user);
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable error) {
            // Error handling
            ...
        }
    });
后台代码和前台代码全都写在一条链中，明显清晰了很多。

更多的例子请看：
[RxJava_Introduct](https://github.com/harichen/RxJava_Introduct/)。好，Retrofit 部分就到这里

####2. RxBinding
RxBinding 是 Jake Wharton 的一个开源库，它提供了一套在 Android 平台上的基于 RxJava 的 Binding API。所谓 Binding，就是类似设置 OnClickListener 、设置 TextWatcher 这样的注册绑定对象的 API。

举个设置点击监听的例子。使用 RxBinding ，可以把事件监听用这样的方法来设置：

	Button button = ...;
	RxView.clickEvents(button) // 以 Observable 形式来反馈点击事件
    .subscribe(new Action1<ViewClickEvent>() {
        @Override
        public void call(ViewClickEvent event) {
            // Click handling
        }
    });
看起来除了形式变了没什么区别，实质上也是这样。甚至如果你看一下它的源码，你会发现它连实现都没什么惊喜：它的内部是直接用一个包裹着的 setOnClickListener() 来实现的。然而，仅仅这一个形式的改变，却恰好就是 RxBinding 的目的：扩展性。通过 RxBinding 把点击监听转换成 Observable 之后，就有了对它进行扩展的可能。扩展的方式有很多，根据需求而定。一个例子是前面提到过的 throttleFirst() ，用于去抖动，也就是消除手抖导致的快速连环点击：

	RxView.clickEvents(button)
          .throttleFirst(500, TimeUnit.MILLISECONDS)
          .subscribe(clickAction);
如果想对 RxBinding 有更多了解，可以去它的 [GitHub 项目](https://github.com/JakeWharton/RxBinding) 下面看看。
####3. 各种异步操作

前面举的 Retrofit 和 RxBinding 的例子，是两个可以提供现成的 Observable 的库。而如果你有某些异步操作无法用这些库来自动生成 Observable，也完全可以自己写。例如数据库的读写、大图片的载入、文件压缩/解压等各种需要放在后台工作的耗时操作，都可以用 RxJava 来实现。

###RxJava 深入学习
对于 Android 开发者来说， RxJava 是一个很难上手的库，因为它对于 Android 开发者来说有太多陌生的概念了。可是它真的很牛逼。推荐一些学习文章：

[RxJava 与 Retrofit 结合的最佳实践](http://gank.io/post/56e80c2c677659311bed9841)

[RxJava使用示例](https://mcxiaoke.gitbooks.io/rxdocs/content/topics/How-To-Use-RxJava.html)

[可能是东半球最全的RxJava使用场景小结](http://blog.csdn.net/theone10211024/article/details/50435325)

[Awesome-RxJava](https://github.com/lzyzsd/Awesome-RxJava)

[Alphabetical List of Observable Operators](https://github.com/ReactiveX/RxJava/wiki/Alphabetical-List-of-Observable-Operators)

















