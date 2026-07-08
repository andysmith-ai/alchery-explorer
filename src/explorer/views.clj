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
a { color: #06c; }

.collections { display: flex; flex-wrap: wrap; gap: .4rem; margin: .2rem 0 1rem; }
.chip { display: inline-flex; align-items: center; gap: .2rem; font-size: 13px;
  padding: .2rem .6rem; border: 1px solid #ddd; border-radius: 999px; color: #555; text-decoration: none; }
.chip.on { background: #06c; color: #fff; border-color: #06c; }
.coll-title { color: #444; margin: .3rem 0 1rem; }
.node-collections { display: flex; flex-wrap: wrap; gap: .4rem; align-items: center; margin: .7rem 0; }
.chip.member { background: #eef3fb; border-color: #dbe6f6; color: #06c; }
.chip.member form { display: inline; margin: 0; }
.chip-x { background: none; color: inherit; border: 0; padding: 0; margin-left: .1rem; cursor: pointer; font-size: 14px; }
.add-collection { display: inline-flex; gap: .3rem; margin: 0; }
.add-collection input { border: 1px solid #ddd; border-radius: 999px; padding: .2rem .6rem;
  font-size: 13px; width: 12rem; flex: 0 0 auto; }
.add-collection button { padding: .2rem .7rem; border-radius: 999px; }

/* node view: a native horizontal slider (trackpad / touch) of vertically-scrolling panels */
.carousel-nav { display: flex; align-items: center; gap: .8rem; margin: 1rem 0 .5rem; }
.carousel-nav button { background: #eee; color: #333; border: 0; border-radius: 6px;
  width: 2rem; height: 2rem; font-size: 1.1rem; cursor: pointer; }
.carousel-label { color: #666; font-size: 13px; }
.carousel-panels { display: flex; overflow-x: auto; scroll-snap-type: x mandatory;
  scroll-behavior: smooth; -webkit-overflow-scrolling: touch; }
.carousel-panels::-webkit-scrollbar { height: 0; }
.panel { flex: 0 0 100%; scroll-snap-align: start; height: 64vh; overflow-y: auto; padding-right: .5rem; }
.panel img { max-width: 100%; border-radius: 6px; }
.body { white-space: pre-wrap; word-wrap: break-word; background: #f7f7f7;
  padding: 1rem; border-radius: 6px; margin: 0; }

/* chunks: a schematic list of numbered fragments */
.chunk { display: flex; gap: .7rem; align-items: flex-start; padding: .55rem 0; border-top: 1px solid #eee; }
.chunk:first-child { border-top: 0; }
.chunk-idx { flex: 0 0 1.6rem; height: 1.6rem; border-radius: 999px; background: #e6eef8;
  color: #06c; font-size: 11px; display: flex; align-items: center; justify-content: center; }
.chunk-body { flex: 1; min-width: 0; }
.chunk-head { font-size: 12px; font-weight: 600; color: #667; margin-bottom: .1rem; }
.chunk-text { white-space: pre-wrap; word-wrap: break-word; color: #333; }
")

(def ^:private carousel-js "
(function(){
  var wrap=document.querySelector('.carousel-panels');
  if(!wrap)return;
  var panels=[].slice.call(wrap.children);
  var label=document.querySelector('.carousel-label');
  function cur(){return Math.round(wrap.scrollLeft/wrap.clientWidth);}
  function upd(){var i=cur();label.textContent=(panels[i]?panels[i].getAttribute('data-label'):'');}
  function go(i){i=Math.max(0,Math.min(panels.length-1,i));wrap.scrollTo({left:i*wrap.clientWidth,behavior:'smooth'});}
  document.querySelector('.carousel .prev').onclick=function(){go(cur()-1);};
  document.querySelector('.carousel .next').onclick=function(){go(cur()+1);};
  wrap.addEventListener('scroll',upd);
  upd();
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

(defn- collection-chips [collections active]
  (into [:nav.collections
         [:a {:class (str "chip" (when-not active " on")) :href "/"} "all"]]
        (map (fn [c] [:a {:class (str "chip" (when (= (:id c) (:id active)) " on"))
                          :href (str "/?collection=" (:id c))}
                      (str (:name c) " · " (:size c))])
             collections)))

(defn home [{:keys [q mode nodes collections collection]}]
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
    (when (seq collections) (collection-chips collections collection))
    (when collection [:h2.coll-title (str "▸ " (:name collection))])
    (if (seq nodes)
      (into [:section.results] (map card nodes))
      [:p.empty (cond collection "empty collection"
                      q          "nothing found"
                      :else      "nothing here yet — ingest a url above")])))

(defn- chunk-panel [chunks]
  (into [:section.panel {:data-label (str "chunks · " (count chunks))}]
        (map (fn [c] [:div.chunk
                      [:span.chunk-idx (:idx c)]
                      [:div.chunk-body
                       (when (seq (:heading c)) [:div.chunk-head (:heading c)])
                       [:div.chunk-text (:text c)]]])
             chunks)))

(defn node [n chunks node-cols]
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
       [:div.node-collections
        (map (fn [c] [:span.chip.member (:name c)
                      [:form {:method "post" :action (str "/node/" (:id n) "/uncollect")}
                       [:input {:type "hidden" :name "collection" :value (:id c)}]
                       [:button.chip-x {:type "submit" :title "remove"} "×"]]])
             node-cols)
        [:form.add-collection {:method "post" :action (str "/node/" (:id n) "/collect")}
         [:input {:name "collection" :placeholder "add to collection…"}]
         [:button "+"]]]
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
