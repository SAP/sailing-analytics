package com.sap.sailing.gwt.ui.client.media;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.desktop.partials.media.MediaPageResources;
import com.sap.sailing.gwt.home.shared.SharedHomeResources;
import com.sap.sailing.gwt.ui.common.client.YoutubeApi;
import com.sap.sse.common.media.MediaSubType;
import com.sap.sse.gwt.client.media.MediaMenuIcon;
import com.sap.sse.gwt.client.media.TakedownNoticeService;
import com.sap.sse.gwt.client.media.VideoDTO;

public class VideoThumbnail extends Composite implements HasClickHandlers {

    private static VideoThumbnailUiBinder uiBinder = GWT.create(VideoThumbnailUiBinder.class);

    private final String videoSourceRef;
    private final Date videoCreateAt;

    @UiField
    SharedResources res;
    @UiField
    MediaPageResources local_res;

    interface VideoThumbnailUiBinder extends UiBinder<Widget, VideoThumbnail> {
    }

    @UiField
    DivElement captionUi;
    @UiField
    ImageElement thumbnailUi;
    @UiField
    Anchor deleteAnchor;
    @UiField
    DivElement overlay;
    @UiField(provided = true)
    MediaMenuIcon videoThumbnailMenuButton;

    public VideoThumbnail(VideoDTO video, String eventName, ClickHandler deleteHandler,
            ClickHandler editHandler, TakedownNoticeService takedownNoticeService) {
        this.videoSourceRef = video.getSourceRef();
        this.videoCreateAt = video.getCreatedAtDate();
        MediaPageResources.INSTANCE.css().ensureInjected();
        SharedHomeResources.INSTANCE.sharedHomeCss().ensureInjected();
        videoThumbnailMenuButton = new MediaMenuIcon(takedownNoticeService, "takedownRequestForEventGalleryVideo");
        videoThumbnailMenuButton.setData(eventName, videoSourceRef);
        initWidget(uiBinder.createAndBindUi(this));
        getElement().addClassName("videoThumbnail");
        captionUi.setInnerText(video.getTitle());
        deleteAnchor.addClickHandler(deleteHandler);
        if (video.getThumbnailRef() == null) {
            if (video.getMimeType().mediaSubType == MediaSubType.youtube) {
                String youtubeId = YoutubeApi.getIdByUrl(video.getSourceRef());
                if (youtubeId != null) {
                    thumbnailUi.setSrc(YoutubeApi.getThumbnailUrl(youtubeId));
                }
            }
            if (thumbnailUi.getSrc().isEmpty()) {
                thumbnailUi.setSrc(
                        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAfoAAAEcCAIAAACgauGBAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyRpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMy1jMDExIDY2LjE0NTY2MSwgMjAxMi8wMi8wNi0xNDo1NjoyNyAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENTNiAoTWFjaW50b3NoKSIgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDowMUY0NzU3REY3RUYxMUU0ODZGRkQyREUyNTUwMzk5QiIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDowMUY0NzU3RUY3RUYxMUU0ODZGRkQyREUyNTUwMzk5QiI+IDx4bXBNTTpEZXJpdmVkRnJvbSBzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOjAxRjQ3NTdCRjdFRjExRTQ4NkZGRDJERTI1NTAzOTlCIiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjAxRjQ3NTdDRjdFRjExRTQ4NkZGRDJERTI1NTAzOTlCIi8+IDwvcmRmOkRlc2NyaXB0aW9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+mRN5aAAAF9dJREFUeNrs3XlsVOX+x/FhZjrTUoplsbIIla0uLAZQFhUwKEtkXwyKUkgqKSWAkdBAKMFUoFQopUALLbSEsiNIEVnFCBi2CorKEoVWoG5AQbauM2253zvPj5Ne8N778wp0zjPv1x/NFP6B5zzn83zP9zxzTrVt27ZZAAC6szIEAEDcAwCIewAAcQ8AIO4BAMQ9AIC4BwAQ9wAA4h4AiHsAAHEPACDuAQDEPQCAuAcAEPcAAOIeAEDcAwCIewAg7gEAxD0AgLgHABD3AADiHgBA3AMAiHsAAHEPACDuAYC4BwAQ9wAA4h4AQNwDAIh7AABxDwAg7gEAxD0AgLgHAOIeAEDcAwCIewAAcQ8AIO4BAMQ9AIC4BwAQ9wCAf2FnCJRq1apZrdbbt28zFLiPk6qiooJJBeK+itk85ENBQUFRUVFOTs7p06f9/Pw4OXG/sr68vDw0NLRNmzYyr4KDg+UPJf3LysoYHB8kc+DWrVtBQUFut5u4f3iDLufhpUuXdu7cmZ2dnZuby0TEw9G4ceP27dv36dOnUaNG8msVnvZ4yJlz+fLlyMjIVq1azZw5k+r+YZRaMuhXr15dsGDBkSNHjD93OBzyV7fvYGrivk88oT7keWRlZcmvcuZHR0fXq1dPrgCk5GegdD36Vqs1Njb20KFDNptNXeER9w/yf2i3S44fPHhw1qxZKtCdTqd8qPCQCks+q8RnduIBnfNqjlnvkM8nT54cOXKk/O348eN79eols5Qmj2aklPziiy/i4+Plc40aNYqKiqr8EGse93JqrVixYv369Wr05acUUy6X666IJ+vx4KjZZVxByjkvc8+4dbTIo0ePHpL7EvpU+nqUmJcvX544ceKVK1ckduSYlpSUqCNbtVGj7UZMGeXvvvuuX79+kvX+/v5+fn5SyEvQGzsliHhUYfTLPJTZKHNSZqbMzz179vTv33/v3r3yK6NkarKKy/odHh5+9epVSSE5xGqB94ryV8tr58LCwnfeeScmJkaqezl/SktLjdtipDy8J/ctnhu2Mj/9PObOnfvmm29KSagKf5iuxPzhhx/69Omzfft2WcIliGRFt9zp5hH395+cMwcPHhw2bFheXp7T6ZR1laCHKUJf5qrkxbVr10aNGrVp0ybVe4Qp2O32goKCiIiISZMmyVItKaS6N8Y2EG/5d2qW9dOnT8/OzpalVRVNal8EQQ8vD311sS/FoISFZEdGRsb+/fsXLVpUXl7O+Hg5q9W6ePHirVu3WjzbQCrfGvS25NGquo+MjJSsDwgIkKVVzhPvHHHgTxNfZYTMWylTJDVycnKGDx8unxkcr6W6N/369ZOslxJTlmp1vLy2xNQk7gsLC8PDw8+fPy/nSXFxMUU9zFvmC9XNv379+ltvvXXp0iUvudEHgyS7VPETJkyYNGmSukFoihJTh7iXs+Ltt9+Wn+quLEU9NCjz3W63ZIr8jIiI4LvfXsVut3/yySeDBw+W0l6KenWDUC3JXh47po97WVFlgZVRVucGRT20KfPVBj4pYqZNm3bjxg2Gpco5HI5z5869/vrrqampTqdTMkeKeosXd2+0intZZqOjo+VqV66nyHpomfjyoaCgQGoavoFVhVRf/r333hs/fnxxcbHkvsvlMt0NQhPHvdqHc+bMGTX0ZD10TXzJmt9//12yht2ZVVVWbt26dciQIadOnfL391dfkTOOkYn+I2aNeynns7KyvvrqKxl9o1/PvISuiS/FTW5u7uzZsyV6GJaHRtZXWWjDw8OXLFmi9t6UlJSY9ylbZp06cgxSU1Mr35tlakLXxLd47lFJ0O/du/fVV1999tlnGZYHTe2Pio+PP3DggMp9+dXsj1M0ZXUv816VOezAgY9Ej3rWk8z55ORk5vyDJnXkvn37Bg8eLFkvRb3VajVp98b0cS9Tf8eOHTk5Oeo7KZT28IUCXyW+uq5NS0uTAGJYHgSp4n/99deRI0fOnTvX6XTK+mrq7o3p414uqVauXCnLr2Q91T18J/HVczRVS+fixYuMyX1XVlY2c+bMyMhIGV61AUTthdUmZ0wW9+pi9vr166rY4duG8B3GnC8sLExKSmKXzn0k5WN2dvbgwYO//PJLnbo3d+enuf65N2/e3L9/v4Q+u+zhgwW+sS/zxIkTZ8+eDQ0NZVj+ftD/8ssvsbGxeXl5Eixq742uT2ExU3UvS+7u3bulwJEPtHHgm4mvvkAunzdt2sSmzL+pvLx88eLFERERkvVytSS/GnWklvFiN9ex2bx5s8x1o6HGfIWvUQW+VDxHjx69cuVKlb/t2qzBZ7cfPHgwLi5OUkU9L119VVPvYDFTdS9Xr9euXVPv8yTr4bMFvrphW1BQcPjwYQbkfwj6q1evRkZGzpgxw+Jp5njnq0h8Ou7lUmvlypXGc6OYtfDxAl9+7t69m37OXyKxnpGRMXLkyAsXLjidTvlV7+7N3UudWf6ht27d+vbbb2UpVg+NYuLCxwt8q9Wak5Nz/vz5xx9/nDH5ryQ6jh8/PmvWLEkS1ab3wbfdmSbuz507J/NbPeWYuQsSX84F+Xnq1Cni/r9knN1++fLl+Pj406dPqwdKS4Zo882pv8QczRw5Qjt27JBjQycHsFR6rMKBAwfYgP8fqO5NeHi4ZL3T6VTvjTGWTJ9b+cxSy0gVc/sOJjGo7pXc3NyioiI6+PeSVfDYsWMffvjhjRs3fLZ7Y8q4d7lccjmmHpzAPAZU3Sopf/PmzevXr9etW5cBqXzpc+vWrXnz5h09elT1Bny2e2PKuP/tt98snhfK0LgHjAJfzggpgM6cOUPcV876LVu2pKeny/g4nU63hyleJEvc/5/8/HxVzrA+A0auqWdkqrMDqnuTnJws1aHqbtG9MV/cSwkjx08dMA4bYFT36qecHapf4cujcePGjaSkJNW9kdyne2PiuD9x4oSq7jlgwF2Jn5eX58uDIPmwc+dOKeolH1T3Rj0OgerQlHGvenDq6pUDBtxFSnvfPDWkij958mRKSkpubq6Evt1up3tj+rg3epQcQuCuU8PieVKsD/7fb9y4kZ6evnfvXgvdG53iHsC/u/BVwedTfU4p5Ldt25aamlpeXi5XNvJ/p3tD3AM+wXeqe7vd/v333y9dujQnJ0f+1/JrWVmZusoh6Il7QGeqpA0KCvKFxC8uLl61atXWrVstd7o3vPeCuAd8hUo67b9qbrPZPv/887S0tIKCAqno5X9N94a4B6Bb0P/4448pKSlnz561ePYg0b0h7gHopqioKDMzc/v27RZP177yq0gYHOIegA6kit+5c2dGRsatW7fUe9hVw4qsJ+4BaELCPS8vb9GiRSdPnrRU6t5YaNMT9wC04Xa7k5OTd+3aZaF7Q9wD0JJU8Xv37s3IyMjPz5d8t1qtdG+IewBakWS/cOFCSkqK6t5IUV/uQdAT9wC0Mn/+/D179lg8XfvKL6Mm64l7ADpwOByS8uvXr//5559V1qs3rVPUE/cA9JGfn5+cnPz1119b7tySNb4YTNYT9wA0sXTp0s8++6ykpMRqtUotL0FPxBP3APThcDh27dq1cePGyt0b9dxmGjjEPQBN5OfnL1y48Pjx45Y7t2Qr77Mk64l7AObPF7s9PT19+/btxcXFqntT+ZYsQU/cA9Ah6A8fPrxq1aqffvrJQveGuAegpfz8/JSUlGPHjlno3hD3AHQt6tPS0nbv3l1UVGS5Z0M9QU/cA9CBlPOZmZnnzp0zgp5H3xD3ALRSUFCwcOHCQ4cOWejeEPcAtOTn57du3botW7Zcv37dQveGuAegpePHj2dkZJw/f97iebblXUU940PcAzC9wsLCxMTE7OxsFe5GLU9RT9wD0ITD4diwYcPHH3+sujeqqGdDPXEPQCs5OTkpKSlnz57906An64l7AKYnsT5r1qxvvvnG7XYbWU/3hrgHoA+Hw7Fx48YtW7bk5+db6N4Q9wD0I2mem5ublJSknntD94a4B6CtuLi4ixcvStBbPK+aontjalaGAMCfcrlcbrfbeHCxhe4N1T0APYtBq1U9EYGgp7oHoLPbd1h4aThxDwAg7gEAxD0AgLgHABD3AADiHgBA3AMAcQ8AIO4BAMQ9AIC4BwAQ9wAA4h4AQNwDAIh7AABxDwDEPQCAuAcAEPcAAOIeAEDcAwCIewAAcQ8AIO4BAMQ9ABD3AADiHgBA3AMAiHsAAHEPACDuAQDEPQCAuAcAEPcAQNwDAIh7AABxDwAg7gEAxD0AgLgHABD3AADiHgBA3AMAcQ8AIO4BAMQ9AIC4BwAQ9wAA4h4AQNwDAIh7AABxDwDEPQCAuAcAEPcAtFHtDvWZASHuAeipoqKivLxcJf7t27eN6AdxD0ArDofDz89PQl+y3mr9Z1ao0GdkiHsAuomJiWnWrFmFh1HmW+jtEPcAdCLJ3qRJkyVLlowZMyYkJMQo8wl94h6AhlwuV9++fdPT0zt37uxwOO4KfRKfuAegV1JYrTExMQkJCWFhYRbPXVx6O8Q9AG01bdo0OTk5IiKiVq1a9HaIewA6c7lcgwYNSk1N7dy5syrzVe1Pb4e4B6ChwMDAmJiYuLi4Jk2aSMrT2yHuAeisTZs2UuZX7u3YbDbKfOIegIbcbrfq7bz00kvya3l5udWDMp+4B6ChwMDAKVOmzJgxo2nTppL4UukbZT6hT9wD0E3btm0XL148dOhQSX8JfXo7xD0AbZWVlY0aNUpCv0OHDhZ6O8Q9AL3VqVPngw8+iI2NbdasGb0d4h6A5mV++/btU1JShg0bFhAQoB6kTG+HuAegbeiPGDEiNTW1Xbt26un5kvj0doh7AHqqU6dOfHx8dHR048aNZQGQ3Lfb7fR2iHsAGnK5XN26dVuyZMmgQYP8/f0l9KXGr9zQB3EPQB+S7BEREWlpac8991y5h+rtMDLEPQAN1alTJy4ubvLkyY0aNVK9HVXmW2jsEPcANONyubp06ZKamtq7d2/Lv27PJ/GJewC6kXCfMGFCWlpa69at1fZ8Pz8/9VeEPnEPQCsS8Q0bNkxISJg6dWpISIjb7ZY/tNlsKvEJfeIegFYk5V944YWlS5f26dNHSn51C5cvZBH3APRkt9vHjRu3bNmy1q1bq1u4qrdDmU/cA9CN1PX169dPSEiYOHFiUFCQVP3qFi6hT9wD0JCkfPfu3ZcvX96/f39jez69HeIegJ4CAgKioqIWLlwYFhZGb4e4B6AzqeubNm0qiR8dHV2jRg2p+o0nrJH4xD0A3Uhp361btxUrVgwYMEA+yxrgcDjUXxH6xD0A3fj7+0dFRSUkJLRo0cLlcknQ2+12C70d4h6AlmX+U089tWjRogkTJkjEy6+S+PR2iHsA2oZ+z549V61a1b17d7fbTW9Hq7iXpdvYeMsBAwy+/BTJmjVrTp48OSkpqXnz5i6XSyJCKn1V5hMUJo57OXilpaUq9zlgQOVKyOLZpe6bp4b8xyXrk5OTR48erap+p9PJ9nxzx71crLVu3fqf/1ZegwDcU903btzYlwdBUn7gwIGrV6/u2LGj1IUVFRX0dswd9w0aNODtB8C9WS8/5exQD5L05aucoKCgGTNmxMfHN2zY0OVy2Ww2ejumjHsREhKiqnv6OYCRcep699FHH2U0LJ5XprRq1WrZsmVjxoyRGp/ejlnjvn79+pY7r7zhmAGq+pEzQrIsLCyM0TBI0Pfr12/dunUdOnSgt2PKuJcDpl59wAEDjPCSArZmzZrBwcGMxl3XPYGBgTNnzoyPj5fBkarfz8/PZrPR27GaZWa3bNmy2h1MaJD1SrNmzapXr86A3Ev1dlauXPnGG2/IZ7kSordjjriXuv61115TX5umfQ+oxr2cEV26dJEsY0D+bcBZraNGjVq9erXUi6WlpTJuvtzbMU0rvEmTJhUVFbJEU90Dchaod3lLijEa/1lZWVlwcHBiYuKcOXOCgoJ8ubdjmriX49S2bVs5cupFB0xi+HLWS9EqWd+8efPQ0FAG5P/ZIXjmmWfWrFlTubejrpN8J09ME/dyhEaMGCHHhn4OfJxxFvTq1UsKIAbkL62Uo0aNWrly5RNPPFFaWiqrplT6vlPmm2lfY4sWLWrVqiXzW3Utmbvw2dJezoIaNWp07tyZAfmrZOhq166dlpY2ffp0VfX7+/sbj9XUO1jMFPc2m23w4MFyFUaBDx8v7SsqKjp06FC3bl0G5H8jKS8DmJWVNWjQoJKSEuOxmnr3dswU9zLFe/furbqW7MiEb5b26iatfB4yZAidnL8bf1brmDFjli9fHhoaqh69oHdvx2RfUg0KCurevbvMcuOoMGXhO1mvSnuJ+zZt2rRo0YIxuS9lfkhIyNKlS99//30ZXtXb0XV7vsniXoJ+7NixwcHBat8xLR34DmPOBwYGvvvuu2y3v7+h//zzz2/evLlbt24lJSW6PnrBfI+gkcMQHh6u3klPSwe+U9obN2lfeeWVevXqMSb3nURKTExMWlpa/fr1ZTV1Op2aPVbTfHEvo9+nT5/mzZvLB+O7EsxU6J31xvMvGzRoMHr0aCk/GZYHQVK+YcOGK1asmDx5spT5srjq1Nsx5QMmpbSfOnWqHAmqe/gC45EJMufHjRvHnH8ICdO1a9esrCz5qVNvx6zPE5aL2aioKDkqcsFFgQ+9S3shF7KS9T169GjXrh1j8nCWWD8/v5iYmGXLlj322GOqt2P2Ry+YNe5lvR04cGDHjh1l7SXxoXHWq9046r2s0dHRbL58yGV+/fr1MzMzx44dq7bnm7q3YzX1kYiNjX3yySdl4ZVLLRIfuma9pIyEzvz589mNUyVkie3Xr9/mzZtbtWoloW+1Wk3a27Ga/TDMmTNHLrWk2GcnPrTMevkQFBQ0b9485nYVUl+7TUxMTElJqV69uioxTdfbMf27AGXEExIS1FcNSXxolvXqiz8zZsyoWbMmw1LlJOVDQ0M/+uijqKio0tJS1duxmOfRCzq8+jU4OHj16tW1atWqfOeW0IdJg15lvdQuqoJZvnx5s2bNGBmvair0798/Kyvr6aefLikpkVVZFZoWr+/taPKm78DAwMzMzCZNmsiSGxAQcNuDxIcZi3ohVYvULlLBrF27NiQkhG+Pexu1Ei9YsCAxMdF49IL393asOh2DtLS0Tp06FRcXm2LogXuLepm3kvVStbRo0WLNmjXGLUF4IZfLFRYWtnXr1gEDBqh9O17+yhSrZqMfGxs7bdo0Uww9ULmot3geEFJRUSFZHxERkZKSop58CS8nhywqKmrDhg2NGzeWY+fNvR2rZkMvV1WdO3feuHFjaGioMfTGScXUhLcFvfogs1TmqtQrtWvXzszMHDp0KHsuTaSsrCwwMDA9PT0xMVEWaa99ZYpVv6GXIQ4ICFi2bNns2bNVW03KfEIfXhv0qlMvJk+evHbt2jp16lDXm7S7EBYWtn379r59+5aUlEj4eNsrU6waD33r1q0//fTT4cOHy9DLuSTnlYy+8eJDch9VlfKq4pPZKHNSZqbMz549e8pc7dq1q/zKKJmaLNXjxo1bvXp13bp1JYXUdZuX3Gyvtm3bNr1H32azyc9Dhw7NmjVLPUdQbdas8DD28LD5AQ8u4o2LequHfCgtLVV/O2HChF69eqlH4jBWOpHlfN++fXFxcfK5Ro0aRUVFL7/88pQpU6qwTWf3hcVWfnbq1GnXrl1//PFHUlLSkSNHKh8SY/cbiY8HkfVGRS8Rbzy4WC49J02aVK9ePZmf8odkvZYNhhdffHHnzp0zZ848ePCgrOhS5lPdP1RybSUn3uXLl+UwZGdn5+TkMC/xcISGhrZv375v374NGza0eLYVMCa+QFL+ypUrkZGRLVu2lOivwure5+LeIIutuqwuLCwsLi7Ozc09f/68HAlqfNyvul5qdon4Nm3aSJHxyCOPqGtNbsP6JofDcfPmzaCgoCpc5n037v90ASDrcX/RJIQXXWcwBAbKLgAaszIEAEDcAwCIewAAcQ8AIO4BAMQ9AIC4BwAQ9wAA4h4AiHsAAHEPACDuAQDEPQCAuAcAEPcAAOIeAEDcAwCIewAg7gEAxD0AgLgHABD3AADiHgBA3AMAiHsAAHEPACDuAYC4BwAQ9wAA4h4AQNwDAIh7AABxDwAg7gEAxD0AgLgHAOIeAEDcAwCIewCAd/uHAAMA2WtK4DMRjqsAAAAASUVORK5CYII=");
            }
        } else {
            thumbnailUi.setSrc(video.getThumbnailRef());
        }
    }

    public boolean isVideo(VideoDTO video) {
        return video != null && video.getCreatedAtDate().equals(videoCreateAt)
                && video.getSourceRef().equals(videoSourceRef);
    }

    @Override
    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return addDomHandler(handler, ClickEvent.getType());
    }

    public void setManageable(boolean manageable) {
        if (manageable) {
            overlay.getStyle().setDisplay(Display.BLOCK);
        } else {
            overlay.getStyle().setDisplay(Display.NONE);
        }
    }

}
