package com.master.info_ua.videoannottool;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.master.info_ua.videoannottool.adapter.SpinnerAdapter;
import com.master.info_ua.videoannottool.adapter.VideosAdapter;
import com.master.info_ua.videoannottool.annotation.Annotation;
import com.master.info_ua.videoannottool.annotation.Audio;
import com.master.info_ua.videoannottool.annotation.ControlerAnnotation;
import com.master.info_ua.videoannottool.annotation.DirPath;
import com.master.info_ua.videoannottool.annotation.Video;
import com.master.info_ua.videoannottool.annotation.VideoAnnotation;
import com.master.info_ua.videoannottool.annotation_dessin.DrawView;
import com.master.info_ua.videoannottool.annotation_dialog.DialogAudio;
import com.master.info_ua.videoannottool.annotation_dialog.DialogText;
import com.master.info_ua.videoannottool.fragment.Fragment_annotation;
import com.master.info_ua.videoannottool.fragment.Fragment_draw;
import com.master.info_ua.videoannottool.menu.DialogImport;
import com.master.info_ua.videoannottool.menu.DialogProfil;
import com.master.info_ua.videoannottool.menu.DialogShare;
import com.master.info_ua.videoannottool.util.Categorie;
import com.master.info_ua.videoannottool.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.master.info_ua.videoannottool.annotation.AnnotationType.*;
import static com.master.info_ua.videoannottool.util.Util.parseJSONAssets;
import static com.master.info_ua.videoannottool.util.Util.saveVideoAnnotation;

public class MainActivity extends Activity implements Ecouteur, Fragment_draw.Listener_fonction, DialogAudio.DialogRecordListener, DialogText.DialogTextListener, Fragment_annotation.AnnotFragmentListener {

    private Menu mainMenu;

    private ImageButton audioAnnotBtn;
    private ImageButton textAnnotBtn;
    private ImageButton graphAnnotBtn;
    public static RelativeLayout btnLayout;

    // Attribut en lien avec exoplayer
    // le player et son mediaSource
    private SimpleExoPlayer player;
    private SimpleExoPlayerView playerView;
    private MediaSource videoSource;

    // attribut servant pour l'option de pleine écran du lecteur a stocker des iinformation
    private final String STATE_RESUME_WINDOW = "resumeWindow";
    private final String STATE_RESUME_POSITION = "resumePosition";
    private final String STATE_PLAYER_FULLSCREEN = "playerFullscreen";

    private int ResumeWindow;
    private long ResumePosition;

    // attribut pour les bouton associé au pleine écran
    private boolean ExoPlayerFullscreen = false;
    private FrameLayout FullScreenButton;
    private ImageView FullScreenIcon;
    private Dialog FullScreenDialog;

    // attribut pour les bouton associé a la répétition
    private boolean ExoPlayerRepeat = false;
    private FrameLayout RepeatButton;
    private ImageView RepeatIcon;

    // attribut pour les bouton gérant le ralentit de la vidéo
    private float exoplayerSpeed = 1f;
    private FrameLayout SpeedButton;
    private ImageView speedIcon;

    // aqttribut pour gérer le bouton play
    private boolean exoplayerPlay = false;
    private FrameLayout playButton;
    private ImageView playIcon;

    private ListView listViewVideos;

    private Spinner spinnerCategorie;
    private Spinner spinnerSubCategorie;

    private VideosAdapter videosAdapter;

    private List<Video> videoList;
    private Video currentVideo;

    String videoName; // a modifié pour aller chercher le nom des video

    private Fragment_draw drawFragment;
    private Fragment_annotation annotFragment;
    private static final String FRAGMENT_DRAW_TAG = "drawFragment";
    private static final String FRAGMENT_ANNOT_TAG = "annotFragment";

    private FragmentManager fragmentManager;

    private DrawView drawView;
    private ImageView drawBimapIv;
    private TextView annotCommentTv;

    // controler pour excécuter les annotation au moment voulu
    // handler servant a récuperer les messages des threads secondaire et as les effectuer dans le main thread
    private ControlerAnnotation controlerAnnotation;
    private Handler mainHandler;


    private VideoAnnotation currentVAnnot;
    private Categorie currentCategorie;
    private Categorie currentSubCategorie;

    private ArrayAdapter<Categorie> spinnerAdapter;
    private ArrayAdapter<Categorie> spinnerAdapter2;

    public static final boolean ELEVE = false;
    public static final boolean COACH = true;
    private boolean statut_profil = ELEVE;            //flag pour savoir si utilisateur = eleve ou coach. L'app se lance en eleve


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Autorisation pour les droit de lecture et d'écritures des fichier
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
            }
        }
        //Autorisation enregistrement audio
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // récupération des donnée pour les transmettre via une instance lors du passage en mode pleine écran
        if (savedInstanceState != null) {
            ResumeWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW);
            ResumePosition = savedInstanceState.getLong(STATE_RESUME_POSITION);
            ExoPlayerFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN);
        }

        listViewVideos = findViewById(R.id.lv_videos);

        spinnerCategorie = findViewById(R.id.spinner_cat);
        spinnerSubCategorie = findViewById(R.id.spinner_sub_cat);


        videosAdapter = new VideosAdapter(this, new ArrayList<Video>());

        listViewVideos.setAdapter(videosAdapter);
        listViewVideos.setClickable(true);
        listViewVideos.setOnItemClickListener(videoItemClickListener);

        listViewVideos.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        //Spinner catégorie
        List<Categorie> categorieList = new ArrayList<>();
        categorieList.add(new Categorie("Categorie", null, "/"));
        categorieList.addAll(setCatSpinnerList());

        spinnerAdapter = new SpinnerAdapter(this, android.R.layout.simple_spinner_item, categorieList);

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorie.setAdapter(spinnerAdapter);

        //Spinner sous-catégorie

        List<Categorie> spinnerList2 = new ArrayList<>();
        spinnerList2.add(new Categorie("Sous-categorie", null, "/"));


        spinnerAdapter2 = new SpinnerAdapter(this, android.R.layout.simple_spinner_item, spinnerList2);
        spinnerAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubCategorie.setAdapter(spinnerAdapter2);


        //Listener btn audio_annot_btn
        audioAnnotBtn = findViewById(R.id.audio_annot_btn);
        audioAnnotBtn.setEnabled(false);        //bouton desactivés de base
        audioAnnotBtn.setOnClickListener(btnClickListener);

        textAnnotBtn = findViewById(R.id.text_annot_btn);
        textAnnotBtn.setEnabled(false);        //bouton desactivés de base
        textAnnotBtn.setOnClickListener(btnClickListener);

        graphAnnotBtn = findViewById(R.id.graphic_annot_btn);
        graphAnnotBtn.setEnabled(false);        //bouton desactivés de base
        graphAnnotBtn.setOnClickListener(btnClickListener);

        btnLayout = findViewById(R.id.btn_layout_id);
        drawBimapIv = findViewById(R.id.draw_bitmap_iv);
        annotCommentTv = findViewById(R.id.annot_comment_tv);

        fragmentManager = getFragmentManager();
        annotFragment = (Fragment_annotation) fragmentManager.findFragmentByTag(FRAGMENT_ANNOT_TAG);

        if (annotFragment == null) {
            annotFragment = new Fragment_annotation();
            fragmentManager.beginTransaction().replace(R.id.annotation_menu, annotFragment, FRAGMENT_ANNOT_TAG).commit();
        }

        drawView = findViewById(R.id.draw_view);

        // il faut mettre la visibilité a GONE pour pouvoir cliquer sur la vidéo, la visibilitè de la vue est rétablie en lancant la saisie d'une annotation
        drawView.setVisibility(View.GONE);

        if (!Util.appDirExist(this)) {
            Util.createDir(this);
        }

        // récupération d'une instance de Handler correspondant a MainActivity
        mainHandler = new Handler(getApplicationContext().getMainLooper());

        //controlerAnnotation = new ControlerAnnotation(this,this,currentVideo.getVideoAnnotation(),mainHandler)
        // initialisation du controler d'annotation
        if (currentVideo != null) {
            controlerAnnotation = new ControlerAnnotation(this, this, currentVideo.getVideoAnnotation(), mainHandler);
        } else {
            controlerAnnotation = new ControlerAnnotation(this, this, null, mainHandler);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        spinnerCategorie.setOnItemSelectedListener(catItemSelectedListener);
        spinnerCategorie.setSelection(1);
        spinnerSubCategorie.setOnItemSelectedListener(subCatItemSelectedListener);

        //videoList = initVideoList();
        //videoList = setVideoList(currentSubCategorie.getPath());
        videoList = setVideoList(DirPath.CATEGORIE1_SUB1.toString());
        if (videoList.size() > 0) {
            currentVideo = videoList.get(0);
            //currentVAnnot = currentVideo.getVideoAnnotation();
            setCurrentVAnnot();
            videoName = currentVideo.getFileName();

            //Affichage de la liste des annotation de la vidéo courante
            annotFragment.updateAnnotationList(currentVAnnot);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        mainMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_import:
                DialogImport dialogImport = new DialogImport();
                dialogImport.showDialogImport(MainActivity.this);
                return true;
            case R.id.action_share:
                //DialogShare dialogShare = new DialogShare();
                //dialogShare.showDialogShare(MainActivity.this);
                return true;
            case R.id.action_profile:
                if (statut_profil == ELEVE) {
                    //DialogProfil dialogProfil = new DialogProfil();
                    //dialogProfil.showDialogProfil(MainActivity.this);
                    item.setTitle("Passer en mode athlète");
                    btnLayout.setVisibility(View.VISIBLE);
                    statut_profil = COACH;

                } else if (statut_profil == COACH) {
                    btnLayout.setVisibility(View.GONE);
                    item.setTitle("Passer en mode coach");
                    statut_profil = ELEVE;
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    // methode pour sauver l'instance contennant la position de la vidéo, l'id de la video et son état
    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt(STATE_RESUME_WINDOW, ResumeWindow);
        outState.putLong(STATE_RESUME_POSITION, ResumePosition);
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, ExoPlayerFullscreen);

        super.onSaveInstanceState(outState);
    }


    // initialise le bouton fullscreen
    private void initFullscreenDialog() {

        FullScreenDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            public void onBackPressed() {
                if (ExoPlayerFullscreen)
                    closeFullscreenDialog();
                super.onBackPressed();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        // attribut du player
        if (playerView == null) {

            playerView = findViewById(R.id.player_view);
            initFullscreenDialog();
            initFullscreenButton();
        }

        // Methodes d'initialisation des bouton
        initSlowButton();
        initExoPlayer();
        initRepeatButton();
        initPlayButton();

        // methodes de lancement en mode plein écrans
        if (ExoPlayerFullscreen) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
            FullScreenDialog.addContentView(playerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            FullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_fullscreen_skrink));
            FullScreenDialog.show();
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        // gestion du player dans le on pause
        if (playerView != null && playerView.getPlayer() != null) {
            ResumeWindow = playerView.getPlayer().getCurrentWindowIndex();
            ResumePosition = Math.max(0, playerView.getPlayer().getContentPosition());

            playerView.getPlayer().release();
        }

        if (FullScreenDialog != null)
            FullScreenDialog.dismiss();
    }

    /**
     * Listener pour le clic sur la liste de vidéos
     */
    protected AdapterView.OnItemClickListener videoItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            videosAdapter.setSelectedListItem(position);
            videosAdapter.notifyDataSetChanged();

            currentVideo = (Video) listViewVideos.getItemAtPosition(position);
            setCurrentVAnnot();
            if (currentVAnnot == null) {
                currentVAnnot = Util.createNewVideoAnnotation();
            }


            annotFragment.updateAnnotationList(currentVAnnot);

            videoName = currentVideo.getFileName();


            player.stop();
            initExoPlayer(); // recrée le lecteur
        }
    };

    // methode pour créer et generer le lecteur
    public void initExoPlayer() {

        SimpleExoPlayerView exoPlayerView = findViewById(R.id.player_view);

        // création du lecteur
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        // préparation de la source
        String filePath;

        if (currentSubCategorie != null && !currentSubCategorie.getPath().isEmpty()) {
            filePath = this.getExternalFilesDir(currentSubCategorie.getPath() + File.separator + videoName).getAbsolutePath();
        } else {
            filePath = this.getExternalFilesDir(DirPath.CATEGORIE1_SUB1.toString() + File.separator + videoName).getAbsolutePath();
        }
        //Uri uri = Uri.fromFile(new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "Camera" + File.separator + videoName + ".mp4"));
        Uri uri = Uri.fromFile(new java.io.File(filePath + File.separator + videoName + ".mp4"));

        DataSpec dataSpec = new DataSpec(uri);
        FileDataSource fileDataSource = new FileDataSource();
        try {
            fileDataSource.open(dataSpec);
        } catch (FileDataSource.FileDataSourceException e) {
            e.printStackTrace();
        }
        videoSource = new ExtractorMediaSource(
                uri,
                new DefaultDataSourceFactory(this, "ua"),
                new DefaultExtractorsFactory(), null, null);

        // initialisation du player avec les instance précedante
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, new DefaultLoadControl());
        exoPlayerView.setControllerShowTimeoutMs(0);
        exoPlayerView.setPlayer(player);
        setSpeed(1f);
        player.setPlayWhenReady(false);
        if (!ExoPlayerRepeat) {
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
        } else {
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
        }
        player.prepare(videoSource, false, false);
    }

    // methode permettant de modifier la vitesse du player
    public void setSpeed(float speed) {
        PlaybackParameters speedParam = new PlaybackParameters(speed, speed);
        player.setPlaybackParameters(speedParam);
    }

    // methode pour lancer le mode pleine écran
    private void openFullscreenDialog() {
        ((ViewGroup) playerView.getParent()).removeView(playerView);
        FullScreenDialog.addContentView(playerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        FullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_fullscreen_skrink));
        ExoPlayerFullscreen = true;
        FullScreenDialog.show();
    }

    // methode d'initialisation du bouton repéter
    private void initRepeatButton() {
        PlaybackControlView controlView = playerView.findViewById(R.id.exo_controller);
        RepeatIcon = controlView.findViewById(R.id.exo_repeat_icon);
        RepeatButton = controlView.findViewById(R.id.exo_repeat_button);
        RepeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ExoPlayerRepeat) {
                    // active le mode repeat
                    player.setRepeatMode(Player.REPEAT_MODE_ONE);
                    RepeatIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.repeat_button_on));
                    ExoPlayerRepeat = true;
                } else {
                    // desactive le mode repeat
                    player.setRepeatMode(Player.REPEAT_MODE_OFF);
                    RepeatIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.repeat_button_off));
                    ExoPlayerRepeat = false;
                }
            }
        });
    }

    // methode d'initialisation du bouton ralentit
    private void initSlowButton() {
        PlaybackControlView controlView = playerView.findViewById(R.id.exo_controller);
        speedIcon = controlView.findViewById(R.id.exo_speed_icon);
        SpeedButton = controlView.findViewById(R.id.exo_speed_button);
        SpeedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (exoplayerSpeed == 1f) {
                    // reduit la vitesse
                    setSpeed(0.5f);
                    speedIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.speed_down));
                    exoplayerSpeed = 0.5f;
                } else {
                    // augmente la vitesse
                    setSpeed(1f);
                    speedIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.speed_up));
                    exoplayerSpeed = 1f;
                }
            }
        });
    }

    // methode d'initialisation du bouton play
    private void initPlayButton() {
        PlaybackControlView controlView = playerView.findViewById(R.id.exo_controller);
        playIcon = controlView.findViewById(R.id.exo_play_icon);
        playButton = controlView.findViewById(R.id.exo_play_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (exoplayerPlay == false) {
                    // lance la video
                    playIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.exo_controls_pause));
                    exoplayerPlay = true;
                    controlerAnnotation.setLast_pos(0);

                    player.setPlayWhenReady(true);
                    new Thread(controlerAnnotation).start();
                } else {
                    // augmente la vitesse
                    playIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.exo_controls_play));
                    exoplayerPlay = false;
                    player.setPlayWhenReady(false);
                    controlerAnnotation.cancel();
                }
            }
        });
    }

    // methode pour fermer le mode pleine écran
    private void closeFullscreenDialog() {

        ((ViewGroup) playerView.getParent()).removeView(playerView);
        ((FrameLayout) findViewById(R.id.main_media_frame)).addView(playerView);
        ExoPlayerFullscreen = false;
        FullScreenDialog.dismiss();
        FullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_fullscreen_expand));
    }

    // initialisation du bouton pleine écran
    private void initFullscreenButton() {

        PlaybackControlView controlView = playerView.findViewById(R.id.exo_controller);
        FullScreenIcon = controlView.findViewById(R.id.exo_fullscreen_icon);
        FullScreenButton = controlView.findViewById(R.id.exo_fullscreen_button);
        FullScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ExoPlayerFullscreen)
                    openFullscreenDialog();
                else
                    closeFullscreenDialog();
            }
        });
    }

    //Initialise la liste de vidéos pous la session ( A BUT DE TESTS )
    protected List<Video> initVideoList() {


        List<Video> videoList = new ArrayList<>(); //Liste de vidéo

        VideoAnnotation videoAnnotations1 = parseJSONAssets(this, "annot_video1.json");
        VideoAnnotation videoAnnotations2 = parseJSONAssets(this, "annot_video2.json");
        VideoAnnotation videoAnnotations3 = parseJSONAssets(this, "annot_video3.json");
        VideoAnnotation videoAnnotations4 = parseJSONAssets(this, "annot_video4.json");

        saveVideoAnnotation(this, videoAnnotations1, DirPath.CATEGORIE1_SUB3.toString(), "video1");

        VideoAnnotation videoAnnotations5 = parseJSONAssets(this, "annot_video1.json");
        ;


        //Création d'instances de vidéos
        Video video1 = new Video("video1", DirPath.CATEGORIE1_SUB3.toString(), videoAnnotations1);
        Video video2 = new Video("video2", DirPath.CATEGORIE1_SUB3.toString(), videoAnnotations2);
        Video video3 = new Video("video3", DirPath.CATEGORIE1_SUB3.toString(), videoAnnotations3);
        Video video4 = new Video("video4", DirPath.CATEGORIE1_SUB3.toString(), videoAnnotations4);
        Video video5 = new Video("video5", DirPath.CATEGORIE1_SUB3.toString(), videoAnnotations5);


        //Ajout dans la liste
        videoList.add(video1);
        videoList.add(video2);
        videoList.add(video3);
        videoList.add(video4);
        videoList.add(video5);


        return videoList;
    }

    /**
     * Construit la liste de vidéo (+annotations associées) contenues dans la sous-catégorie spécifiée en paramètre
     *
     * @param subCatDir
     * @return
     */
    protected List<Video> setVideoList(String subCatDir) {

        List<Video> videoList = new ArrayList<>(); //Liste de vidéo

        /*
        if (isAppDirectory(subCatDir)) {
        } //check if valid directory for app
        */

        File subDirContent = this.getExternalFilesDir(subCatDir);

        if (subDirContent.listFiles().length > 0) {
            for (File videoFileDir : subDirContent.listFiles()) {
                Log.e("SUB_CONT_FILE", videoFileDir.getAbsolutePath());
                if (videoFileDir.isDirectory() && videoFileDir.listFiles().length > 0) {
                    Video video = new Video();
                    for (File videoFile : videoFileDir.listFiles()) {
                        if (videoFile.getName().substring(videoFile.getName().lastIndexOf(".") + 1).equals("mp4")) {
                            Log.e("VIDEO", "Video found [" + videoFile.getName() + "]");
                            video.setFileName(videoFileDir.getName()); //le fichir video porte le même nom que le répertoire qui le contient
                        }

                        if (videoFile.getName().substring(videoFile.getName().lastIndexOf(".") + 1).equals("json")) {
                            VideoAnnotation videoAnnotation = Util.parseJSON(this, subCatDir + File.separator + videoFileDir.getName(), videoFile.getName());
                            video.setVideoAnnotation(videoAnnotation);
                        }
                    }

                    if (video.getFileName() != null && !video.getFileName().isEmpty()) {
                        video.setPath(currentSubCategorie + File.separator + videoFileDir.getName());
                        videoList.add(video);
                    }
                }
            }
        } else {
            Log.e("SUB_CAT", "No content in " + subCatDir);
        }

        return videoList;
    }

    /**
     * initialise la liste d'item du spinner categorie
     *
     * @return
     */
    public List<Categorie> setCatSpinnerList() {
        List<Categorie> categorieList = new ArrayList<>();

        if (Util.appDirExist(this)) {
            for (DirPath dirPath : DirPath.values()) {
                if (!dirPath.isSubDir()) {
                    categorieList.add(new Categorie(dirPath.getName(), null, dirPath.toString()));
                }
            }
        }
        /*else {
            Util.createDir(this);
            categorieList = setCatSpinnerList();
        }
        */
        return categorieList;
    }


    /**
     * initialise la liste d'item du spinner sub-categorie
     *
     * @return
     */
    public List<Categorie> setSubCatSpinnerList(String parentDir) {
        List<Categorie> categorieList = new ArrayList<>();

        categorieList.add(new Categorie("Sous-categorie", null, "../"));
        if (Util.isAppDirectory(parentDir)) {
            Log.e("IS_APP_DIR", parentDir + " IS APP_DIR");
            for (DirPath dirPath : DirPath.values()) {
                if (dirPath.isSubDir() && dirPath.getPath().substring(0, dirPath.getPath().indexOf("/")).equals(parentDir)) {
                    //Log.e("SUB_CAT", dirPath.toString());
                    categorieList.add(new Categorie(dirPath.getName(), parentDir, dirPath.toString()));
                } else {
                    Log.e("IS_APP_DIR", dirPath.toString() + " is not a app dir");
                }
            }
        } else {
            Log.e("IS_APP_DIR", parentDir + " is not a app dir");
        }
        return categorieList;
    }

    /**
     * listener de clic sur les button d'annotation
     */
    protected View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            int btnId = view.getId();
            switch (btnId) {
                case R.id.audio_annot_btn:
                    player.setPlayWhenReady(false);
                    String directoryPath = currentSubCategorie.getPath() + File.separator + videoName;
                    DialogAudio dialog = new DialogAudio(MainActivity.this, directoryPath, player.getContentPosition());
                    Annotation auDdioAnnotation = new Annotation(AUDIO);
                    dialog.showDialogRecord(auDdioAnnotation, videoName);
                    break;
                case R.id.graphic_annot_btn:
                    player.setPlayWhenReady(false);
                    drawView.setVisibility(View.VISIBLE);
                    drawView.setOnTouchEnable(true);
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    drawFragment = (Fragment_draw) fragmentManager.findFragmentByTag(FRAGMENT_DRAW_TAG);
                    if (drawFragment == null) {
                        drawFragment = new Fragment_draw();
                        ft.add(R.id.annotation_menu, drawFragment, FRAGMENT_DRAW_TAG);
                        ft.hide(annotFragment);
                        ft.show(drawFragment);
                        ft.commit();
                    } else {
                        ft.hide(annotFragment);
                        ft.show(drawFragment);
                        ft.commit();
                    }

                    break;
                case R.id.text_annot_btn:
                    player.setPlayWhenReady(false);
                    Annotation textAnnotation = new Annotation("Text Annot ", TEXT);
                    DialogText dialogtext = new DialogText(MainActivity.this, 1);
                    dialogtext.showDialogBox(textAnnotation, MainActivity.this);

                    break;
            }
        }
    };


    /**
     * listener d'écoute pour la sélection d'un item catégorie
     */
    protected AdapterView.OnItemSelectedListener catItemSelectedListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            // Here you get the current item that is selected by its position
            currentCategorie = (Categorie) adapterView.getItemAtPosition(position);

            spinnerAdapter2.clear();
            spinnerAdapter2.addAll(setSubCatSpinnerList(currentCategorie.getPath()));
            spinnerAdapter2.notifyDataSetChanged();
            spinnerSubCategorie.setSelection(1);
            Log.e("SELECT_CAT", currentCategorie.getPath());
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapter) {
        }
    };

    /**
     * listener d'écoute pour la sélection d'un item sous-catégorie
     */
    protected AdapterView.OnItemSelectedListener subCatItemSelectedListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            if (position > 0) {
                // Here you get the current item that is selected by its position
                currentSubCategorie = (Categorie) adapterView.getItemAtPosition(position);
                Log.e("SELECT_SUB_CAT", currentSubCategorie.getPath());

                videosAdapter.clear();
                videosAdapter.addAll(setVideoList(currentSubCategorie.getPath()));
                videosAdapter.notifyDataSetChanged();

                if(videosAdapter.getCount()> 0 ){
                    //currentVideo = videosAdapter.getItem(0);
                    currentVideo = (Video) listViewVideos.getItemAtPosition(0);
                    setCurrentVAnnot();
                    if (currentVAnnot == null) {
                        currentVAnnot = Util.createNewVideoAnnotation();
                    }

                    annotFragment.updateAnnotationList(currentVAnnot);

                    videoName = currentVideo.getFileName();
                    player.stop();
                    initExoPlayer();
                }
            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> adapter) {
        }
    };


    // methode dans Main activity qui renvoie le moment de la position pour les anotation sous forme de long
    @Override
    public long getVideoTime() {
        return player.getCurrentPosition();
    }

    //methode pour fixer le curseur de lecture a une position donné sous forme de long
    public void setVideoTime(long positionCurseur) {
        player.seekTo(positionCurseur);
    }

    @Override
    public SimpleExoPlayer getPlayer() {
        return player;
    }

    @Override
    public void resetCanvas() {
        drawView.resetCanvas();
    }

    @Override
    public void setOnTouchEnable(boolean bool) {
        drawView.setOnTouchEnable(bool);
    }

    @Override
    public String saveDrawImage() {

        String drawfileName = drawView.enregistrer_image(currentSubCategorie.getPath() + File.separator + videoName, this.videoName);

        return drawfileName;
    }


    @Override
    public void onSaveDrawAnnotation(Annotation annotation) {
        // création de l'annotation
        //annotation.setDrawFileName(drawFileName);
        annotation.setAnnotationStartTime(player.getCurrentPosition());
        Log.e("GRAPHIC_ANNOT", "Annotation file name " + annotation.getDrawFileName() + " ==> Annotation title " + annotation.getAnnotationTitle());

        currentVAnnot.getAnnotationList().add(annotation);
        currentVAnnot.setLastModified(Util.DATE_FORMAT.format(new Date()));

        if (currentVAnnot != null && (currentVAnnot.getAnnotationList().size() > 0) && currentSubCategorie.getPath() != null) {
            String directory = currentSubCategorie.getPath() + File.separator + videoName;
            Util.saveVideoAnnotation(MainActivity.this, currentVAnnot, directory, videoName);
            Log.e("GRAPHIC_ANNOT_SAVE", " **** Graphic annot saved successfully ****");
            annotFragment.updateAnnotationList(currentVAnnot);
        } else {
            Log.e("GRAPHIC_ANNOT_SAVE", "One of initialization object is null");
        }

        closeDrawFragment();
    }

    @Override
    public void setColor(int color) {
        drawView.setColor(color);
    }

    @Override
    public void fermer_fragment() {

        drawView.resetCanvas();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        annotFragment = (Fragment_annotation) fragmentManager.findFragmentByTag(FRAGMENT_ANNOT_TAG);
        if (annotFragment == null) {
            annotFragment = new Fragment_annotation();
            ft.add(R.id.annotation_menu, annotFragment, FRAGMENT_ANNOT_TAG);
            ft.hide(drawFragment);
            ft.show(annotFragment);
            ft.commit();
        } else {
            ft.hide(drawFragment);
            ft.show(annotFragment);
            ft.commit();
        }
        drawView.setVisibility(View.GONE);
    }

    @Override
    public void addAudioAnnot(Annotation annotation) {
        Log.i("AUDIO_ANNOT", "Annotation file name " + annotation.getAudioFileName() + " Annotation title: " + annotation.getAnnotationTitle() + "[" + annotation.getAnnotationStartTime() + "]" + " Duration :" + annotation.getAnnotationDuration());

        annotation.setAnnotationStartTime(player.getCurrentPosition());
        currentVAnnot.getAnnotationList().add(annotation);
        currentVAnnot.setLastModified(Util.DATE_FORMAT.format(new Date()));

        if (currentVAnnot != null && (currentVAnnot.getAnnotationList().size() > 0) && currentSubCategorie.getPath() != null) {
            String directory = currentSubCategorie.getPath() + File.separator + videoName;
            Util.saveVideoAnnotation(MainActivity.this, currentVAnnot, directory, videoName);
            annotFragment.updateAnnotationList(currentVAnnot);
            Log.e("AUDIO_ANNOT_SAVE", " **** Audio annot saved successfully ****");
        } else {
            Log.e("AUDIO_ANNOT_SAVE", "One of initialization object is null");
        }
    }

    @Override
    public void addTextAnnot(Annotation annotation) {
        Log.i("TEXT_ANNOT", " Annotation title: " + annotation.getAnnotationTitle() + "[" + annotation.getAnnotationStartTime() + "]" + "Duration : " + annotation.getAnnotationDuration());

        annotation.setAnnotationStartTime(player.getCurrentPosition());
        currentVAnnot.getAnnotationList().add(annotation);
        currentVAnnot.setLastModified(Util.DATE_FORMAT.format(new Date()));

        if (currentVAnnot != null && (currentVAnnot.getAnnotationList().size() > 0) && currentSubCategorie.getPath() != null) {
            String directory = currentSubCategorie.getPath() + File.separator + videoName;
            Util.saveVideoAnnotation(MainActivity.this, currentVAnnot, directory, videoName);
            annotFragment.updateAnnotationList(currentVAnnot);
            Log.e("TEXT_ANNOT_SAVE", " **** TEXT annot saved successfully ****");
        } else {
            Log.e("TEXT_ANNOT_SAVE", "One of initialization object is null");
        }
    }


    public void setStatutProfil(boolean nouveauStatut) {
        statut_profil = nouveauStatut;
    }

    protected void setCurrentVAnnot() {
        currentVAnnot = Util.createNewVideoAnnotation();
        if (currentVideo.getVideoAnnotation() != null) {
            currentVAnnot = currentVideo.getVideoAnnotation();
        }
    }

    /**
     * Ferme la fragment de gestion de l'annotation graphique et affiche celui de la liste de annotations
     */
    protected void closeDrawFragment() {
        drawView.resetCanvas();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        annotFragment = (Fragment_annotation) fragmentManager.findFragmentByTag(FRAGMENT_ANNOT_TAG);
        if (annotFragment == null) {
            annotFragment = new Fragment_annotation();
            ft.add(R.id.annotation_menu, annotFragment, FRAGMENT_ANNOT_TAG);
            ft.hide(drawFragment);
            ft.show(annotFragment);
            ft.commit();
        } else {
            ft.hide(drawFragment);
            ft.show(annotFragment);
            ft.commit();
        }
        drawView.setVisibility(View.GONE);
    }

    @Override
    public void onAnnotItemClick(final Annotation annotation) {

        player.seekTo(annotation.getAnnotationStartTime());
        player.setPlayWhenReady(false);
        final String annotFileDirectory = currentSubCategorie.getPath()+"/"+currentVideo.getFileName();

        Handler loopHandler = new Handler(Looper.getMainLooper());

        switch (annotation.getAnnotationType()){
            case AUDIO:
                //Juste pour récupérer la durée de l'audio
                //Pas nécessaire si "annotation.getAnnotationDuration()" est bien défini
                Uri uri = Uri.parse(this.getExternalFilesDir(annotFileDirectory) + File.separator + annotation.getAudioFileName());
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this,uri);
                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                int duration = Integer.parseInt(durationStr);

                Audio audio = new Audio(this, this.getExternalFilesDir(annotFileDirectory) + File.separator + annotation.getAudioFileName());
                audio.listen();

                loopHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        player.setPlayWhenReady(true);
                    }
                }, duration);  // ==> annotation.getAnnotationDuration()

                break;

            case DRAW:

                Bitmap bitmap = Util.getBitmapFromAppDir(this, annotFileDirectory, annotation.getDrawFileName());
                drawBimapIv.setVisibility(View.VISIBLE);
                drawBimapIv.setImageBitmap(bitmap);

                loopHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        drawBimapIv.setVisibility(View.GONE);
                        drawBimapIv.setImageBitmap(null);
                        player.setPlayWhenReady(true);
                    }
                }, annotation.getAnnotationDuration());

                break;

            case TEXT:
                annotCommentTv.setVisibility(View.VISIBLE);
                annotCommentTv.setText(annotation.getTextComment());

                loopHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        annotCommentTv.setVisibility(View.GONE);
                        annotCommentTv.setText("");
                        player.setPlayWhenReady(true);
                    }
                }, 5000); // ==> annotation.getAnnotationDuration()

                break;

                default:
                    if (drawBimapIv.getVisibility() == View.VISIBLE){
                        drawBimapIv.setImageBitmap(null);
                        drawBimapIv.setVisibility(View.GONE);
                    }
        }
    }
}