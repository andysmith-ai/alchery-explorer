(ns explorer.views
  "Server-rendered HTML for the explorer."
  (:require [hiccup2.core :as h]))

(def ^:private css "
* { box-sizing: border-box; }
body { font: 15px/1.6 system-ui, sans-serif; color: #222; margin: 0; }
header { border-bottom: 1px solid #eee; padding: 1rem 1.5rem; }
.brand { font-weight: 700; font-size: 1.2rem; color: #111; text-decoration: none; }
main { max-width: 820px; margin: 1.5rem auto; padding: 0 1rem; }
.tools { display: flex; gap: .8rem; flex-wrap: wrap; margin-bottom: 1rem; }
.tool { flex: 1; display: flex; gap: .4rem; min-width: 260px; }
input { flex: 1; padding: .5rem .6rem; font: inherit; border: 1px solid #ccc; border-radius: 6px; }
button { padding: .5rem 1rem; font: inherit; cursor: pointer; border: 0; background: #06c; color: #fff; border-radius: 6px; }
.search { display: flex; gap: .6rem; align-items: center; margin: 1rem 0 1.5rem; flex-wrap: wrap; }
.search label { font-size: 13px; color: #555; }
.card { border-top: 1px solid #eee; padding: .8rem 0; }
.meta { display: flex; gap: .6rem; align-items: baseline; }
.kind { color: #999; font-size: 11px; text-transform: uppercase; letter-spacing: .05em; }
.score { color: #08a; font-size: 12px; }
.title { display: block; font-weight: 600; color: #111; text-decoration: none; margin: .1rem 0; }
.title:hover { text-decoration: underline; }
.url a { color: #888; font-size: 12px; word-break: break-all; }
.empty { color: #888; }
.detail .body { white-space: pre-wrap; word-wrap: break-word; background: #f7f7f7;
  padding: 1rem; border-radius: 6px; max-height: 60vh; overflow: auto; }
.chunks h3 { color: #444; margin: 1.2rem 0 .4rem; }
.chunk { border-left: 3px solid #e3e3e3; padding: .2rem 0 .2rem .8rem; margin: .7rem 0; }
.chunk-meta { color: #aaa; font-size: 11px; margin-bottom: .2rem; }
.chunk-text { white-space: pre-wrap; word-wrap: break-word; }
.carousel-nav { display: flex; align-items: center; gap: .8rem; margin: 1rem 0 .6rem; }
.carousel-nav button { background: #eee; color: #333; border: 0; border-radius: 6px;
  width: 2rem; height: 2rem; font-size: 1.1rem; cursor: pointer; }
.carousel-label { color: #666; font-size: 13px; }
.carousel-panels .panel img { max-width: 100%; border-radius: 6px; }
a { color: #06c; }
")

(def ^:private carousel-js "
(function(){
  var panels=[].slice.call(document.querySelectorAll('.carousel .panel'));
  if(!panels.length)return;
  var i=0, label=document.querySelector('.carousel-label');
  function show(){panels.forEach(function(p,j){p.style.display=(j===i?'block':'none');});
    label.textContent=panels[i].getAttribute('data-label');}
  document.querySelector('.carousel .prev').onclick=function(){i=(i-1+panels.length)%panels.length;show();};
  document.querySelector('.carousel .next').onclick=function(){i=(i+1)%panels.length;show();};
  show();
})();
")

(defn- clip [s n] (let [s (or s "")] (subs s 0 (min (count s) n))))

(defn- card [node]
  [:div.card
   [:div.meta
    [:span.kind (:kind node)]
    (when-let [sc (:score node)] [:span.score (format "%.3f" (double sc))])]
   [:a.title {:href (str "/node/" (:id node))} (clip (or (:title node) (:text node)) 160)]
   (when-let [u (:url node)]
     [:div.url [:a {:href u :target "_blank" :rel "noopener"} u]])])

(defn- page [title & body]
  (str "<!doctype html>"
       (h/html
         [:html {:lang "en"}
          [:head
           [:meta {:charset "utf-8"}]
           [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
           [:title title]
           [:style (h/raw css)]]
          [:body
           [:header [:a.brand {:href "/"} "alchery"]]
           (into [:main] body)]])))

(defn home [{:keys [q mode nodes]}]
  (page "alchery explorer"
    [:section.tools
     [:form.tool {:method "post" :action "/ingest"}
      [:input {:name "url" :placeholder "https://…   ingest a url"}]
      [:button "ingest"]]
     [:form.tool {:method "post" :action "/note"}
      [:input {:name "text" :placeholder "…   add a note"}]
      [:button "add"]]]
    [:form.search {:method "get" :action "/"}
     [:input {:name "q" :value (or q "") :placeholder "search the graph"}]
     [:label [:input {:type "radio" :name "mode" :value "semantic"
                      :checked (not= mode "text")}] " meaning"]
     [:label [:input {:type "radio" :name "mode" :value "text"
                      :checked (= mode "text")}] " text"]
     [:button "search"]]
    (if (seq nodes)
      (into [:section.results] (map card nodes))
      [:p.empty (if q "nothing found" "nothing here yet — ingest a url above")])))

(defn- chunk-panel [chunks]
  (into [:section.panel {:data-label (str "chunks (" (count chunks) ")")}]
        (map (fn [c] [:div.chunk
                      [:div.chunk-meta (str "#" (:idx c)
                                            (when (seq (:heading c)) (str " · " (:heading c))))]
                      [:div.chunk-text (:text c)]])
             chunks)))

(defn node [n chunks]
  (let [panels (cond-> []
                 (:image n)   (conj [:section.panel {:data-label "image"}
                                     [:img {:src (:image n) :alt (:title n)}]])
                 :always      (conj [:section.panel {:data-label "full text"}
                                     [:pre.body (or (:text n) "")]])
                 (seq chunks) (conj (chunk-panel chunks)))]
    (page (or (:title n) "node")
      [:article.detail
       [:div.meta [:span.kind (:kind n)]]
       [:h1 (or (:title n) (:id n))]
       (when-let [u (:url n)] [:div.url [:a {:href u :target "_blank" :rel "noopener"} u]])
       [:div.carousel
        [:div.carousel-nav
         [:button.prev {:type "button"} "‹"]
         [:span.carousel-label]
         [:button.next {:type "button"} "›"]]
        (into [:div.carousel-panels] panels)]]
      [:p [:a {:href "/"} "← back"]]
      [:script (h/raw carousel-js)])))

(defn not-found []
  (page "not found" [:p "not found"] [:p [:a {:href "/"} "← back"]]))
