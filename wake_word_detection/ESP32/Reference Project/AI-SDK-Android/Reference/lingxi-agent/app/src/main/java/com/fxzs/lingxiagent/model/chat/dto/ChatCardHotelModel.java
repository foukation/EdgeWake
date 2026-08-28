package com.fxzs.lingxiagent.model.chat.dto;

import java.util.List;

public class ChatCardHotelModel {

    /**
     * moreUrl : https://honor.tscfn.cn/h5/1754978336965_hotel_11.html
     * h5Url : https://m.ctrip.com/webapp/hotels/detail?hotelid=84081438&allianceid=6470339&sid=210062357
     * icon : https://e3f49eaa46b57.cdn.sohucs.com/2025/5/27/11/41/MTAwMTIyXzE3NDgzMTcyNzYxNTY=.png
     * tag : 价格最优
     * star :
     * hotelName : 汉庭酒店(北京顺义中心店)
     * id : B0FFFAEN0Q
     * location : 116.664867,40.133987
     * type : 住宿服务;宾馆酒店;经济型连锁酒店
     * typecode : 100105
     * pname : 北京市
     * cityname : 北京市
     * adname : 顺义区
     * zone : 金汉绿港家园一区10号楼
     * pcode : 110000
     * citycode : 010
     * adcode : 110113
     * business : {"business_area":"胜利","tel":"010-61111567","rectag":"经济型","keytag":"经济型","rating":"4.8"}
     * photos : [{"title":"Logo","url":"https://dimg04.c-ctrip.com/images//0204s12000989a4cnE39E_R_1280_720_Q50.jpg"},{"title":"封面","url":"https://store.is.autonavi.com/showpic/d7841b546e5da125612af127c812b17c"},{"title":"封面图","url":"https://store.is.autonavi.com/showpic/4ecd21b050558ea67de3b8a10dfbfa3e"}]
     * distance :
     * parent : B0FFG4LUTC
     * rating : 4.8
     * price :
     * coverImage : https://dimg04.c-ctrip.com/images//0204s12000989a4cnE39E_R_1280_720_Q50.jpg
     */

    private String moreUrl;
    private String h5Url;
    private String icon;
    private String tag;
    private String star;
    private String hotelName;
    private String id;
    private String location;
    private String type;
    private String typecode;
    private String pname;
    private String cityname;
    private String adname;
    private String zone;
    private String pcode;
    private String citycode;
    private String adcode;
    private BusinessBean business;
    private String distance;
    private String parent;
    private double rating;
    private String price;
    private String coverImage;
    private List<PhotosBean> photos;

    public String getMoreUrl() {
        return moreUrl;
    }

    public void setMoreUrl(String moreUrl) {
        this.moreUrl = moreUrl;
    }

    public String getH5Url() {
        return h5Url;
    }

    public void setH5Url(String h5Url) {
        this.h5Url = h5Url;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getStar() {
        return star;
    }

    public void setStar(String star) {
        this.star = star;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypecode() {
        return typecode;
    }

    public void setTypecode(String typecode) {
        this.typecode = typecode;
    }

    public String getPname() {
        return pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public String getCityname() {
        return cityname;
    }

    public void setCityname(String cityname) {
        this.cityname = cityname;
    }

    public String getAdname() {
        return adname;
    }

    public void setAdname(String adname) {
        this.adname = adname;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getPcode() {
        return pcode;
    }

    public void setPcode(String pcode) {
        this.pcode = pcode;
    }

    public String getCitycode() {
        return citycode;
    }

    public void setCitycode(String citycode) {
        this.citycode = citycode;
    }

    public String getAdcode() {
        return adcode;
    }

    public void setAdcode(String adcode) {
        this.adcode = adcode;
    }

    public BusinessBean getBusiness() {
        return business;
    }

    public void setBusiness(BusinessBean business) {
        this.business = business;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public List<PhotosBean> getPhotos() {
        return photos;
    }

    public void setPhotos(List<PhotosBean> photos) {
        this.photos = photos;
    }

    public static class BusinessBean {
        /**
         * business_area : 胜利
         * tel : 010-61111567
         * rectag : 经济型
         * keytag : 经济型
         * rating : 4.8
         */

        private String business_area;
        private String tel;
        private String rectag;
        private String keytag;
        private String rating;

        public String getBusiness_area() {
            return business_area;
        }

        public void setBusiness_area(String business_area) {
            this.business_area = business_area;
        }

        public String getTel() {
            return tel;
        }

        public void setTel(String tel) {
            this.tel = tel;
        }

        public String getRectag() {
            return rectag;
        }

        public void setRectag(String rectag) {
            this.rectag = rectag;
        }

        public String getKeytag() {
            return keytag;
        }

        public void setKeytag(String keytag) {
            this.keytag = keytag;
        }

        public String getRating() {
            return rating;
        }

        public void setRating(String rating) {
            this.rating = rating;
        }
    }

    public static class PhotosBean {
        /**
         * title : Logo
         * url : https://dimg04.c-ctrip.com/images//0204s12000989a4cnE39E_R_1280_720_Q50.jpg
         */

        private String title;
        private String url;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
