<template>
  <section
    class="hero card fade-up"
    @mouseenter="pauseAutoplay"
    @mouseleave="resumeAutoplay"
  >
    <div class="hero-copy">
      <p class="eyebrow">{{ currentSlide.eyebrow }}</p>
      <transition name="copy-fade" mode="out-in">
        <div :key="currentSlide.id" class="copy-stack">
          <h1>{{ currentSlide.title }}</h1>
          <p class="desc">{{ currentSlide.description }}</p>
          <div class="hero-actions">
            <RouterLink class="btn-primary hero-btn" :to="currentSlide.link">
              进入{{ currentSlide.category }}
            </RouterLink>
            <RouterLink class="btn-outline hero-btn" to="/search">
              浏览全部
            </RouterLink>
          </div>
          <div class="category-pills">
            <RouterLink
              v-for="(slide, index) in slides"
              :key="slide.id"
              :class="['pill', { active: index === activeIndex }]"
              :to="slide.link"
              @mouseenter="goToSlide(index)"
              @focus="goToSlide(index)"
            >
              {{ slide.category }}
            </RouterLink>
          </div>
        </div>
      </transition>
    </div>

    <div class="hero-visual">
      <transition name="visual-fade" mode="out-in">
        <RouterLink :key="currentSlide.id" class="visual-frame" :to="currentSlide.link">
          <img :src="currentSlide.image" :alt="currentSlide.title" />
          <div class="visual-overlay"></div>
          <div class="visual-content">
            <span class="visual-badge">{{ currentSlide.badge }}</span>
            <div class="visual-meta">
              <p class="visual-kicker">{{ currentSlide.kicker }}</p>
              <h2>{{ currentSlide.category }}</h2>
              <p>{{ currentSlide.caption }}</p>
            </div>
          </div>
        </RouterLink>
      </transition>

      <div class="visual-controls">
        <button type="button" class="nav-btn" aria-label="上一张" @click="prevSlide">‹</button>
        <div class="dots">
          <button
            v-for="(slide, index) in slides"
            :key="slide.id"
            type="button"
            :class="['dot', { active: index === activeIndex }]"
            :aria-label="`切换到${slide.category}`"
            @click="goToSlide(index)"
          ></button>
        </div>
        <button type="button" class="nav-btn" aria-label="下一张" @click="nextSlide">›</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";

const slides = [
  {
    id: "phone",
    category: "手机",
    title: "移动科技\n也能有收藏级质感",
    description: "高端数码、利落设计、流畅体验，把每日使用的物件升级成一件值得拥有的作品。",
    badge: "旗舰数码",
    kicker: "Smart Living",
    caption: "点击进入手机分类，查看在售机型与配件。",
    eyebrow: "01 / 05 科技精选",
    link: "/search?category=手机",
    image: "https://images.pexels.com/photos/34241720/pexels-photo-34241720.jpeg?auto=compress&cs=tinysrgb&w=1600&h=900&fit=crop"
  },
  {
    id: "beauty",
    category: "美妆",
    title: "把梳妆台\n变成一块发光展台",
    description: "从日常护肤到精致妆容，挑选配色、包装和成分都足够讲究的美妆单品。",
    badge: "高光美学",
    kicker: "Beauty Edit",
    caption: "点击进入美妆分类，直达精选护肤和彩妆。",
    eyebrow: "02 / 05 质感美学",
    link: "/search?category=美妆",
    image: "https://images.pexels.com/photos/11124449/pexels-photo-11124449.jpeg?auto=compress&cs=tinysrgb&w=1600&h=900&fit=crop"
  },
  {
    id: "books",
    category: "图书",
    title: "给阅读留一块\n安静而高级的空间",
    description: "挑一本值得久放书架的好书，让封面、纸张和内容一起构成你的个人审美。",
    badge: "书香策展",
    kicker: "Curated Reading",
    caption: "点击进入图书分类，浏览近期上架读物。",
    eyebrow: "03 / 05 阅读风景",
    link: "/search?category=图书",
    image: "https://images.pexels.com/photos/1029141/pexels-photo-1029141.jpeg?auto=compress&cs=tinysrgb&w=1600&h=900&fit=crop"
  },
  {
    id: "fruit",
    category: "水果",
    title: "新鲜这件事\n值得被认真陈列",
    description: "把自然色泽和鲜甜口感放进同一张购物清单，让每一次补货都带着丰盛感。",
    badge: "鲜选果物",
    kicker: "Fresh Daily",
    caption: "点击进入水果分类，查看当季鲜果。",
    eyebrow: "04 / 05 新鲜上架",
    link: "/search?category=水果",
    image: "https://images.pexels.com/photos/616833/pexels-photo-616833.jpeg?auto=compress&cs=tinysrgb&w=1600&h=900&fit=crop"
  },
  {
    id: "luggage",
    category: "拉杆箱",
    title: "旅行装备\n不该只有功能感",
    description: "简洁线条、稳重材质、耐用结构，把出行用品从工具升级为风格表达。",
    badge: "旅途器物",
    kicker: "Travel Form",
    caption: "点击进入拉杆箱分类，查看旅行收纳装备。",
    eyebrow: "05 / 05 旅行提案",
    link: "/search?category=拉杆箱",
    image: "https://images.pexels.com/photos/31027055/pexels-photo-31027055.jpeg?auto=compress&cs=tinysrgb&w=1600&h=900&fit=crop"
  }
];

const activeIndex = ref(0);
let autoplayTimer = null;

const currentSlide = computed(() => {
  const slide = slides[activeIndex.value];
  return {
    ...slide,
    title: slide.title.split("\n").join("\n")
  };
});

const clearAutoplay = () => {
  if (autoplayTimer) {
    clearInterval(autoplayTimer);
    autoplayTimer = null;
  }
};

const resumeAutoplay = () => {
  clearAutoplay();
  autoplayTimer = setInterval(() => {
    nextSlide();
  }, 4800);
};

const pauseAutoplay = () => {
  clearAutoplay();
};

const goToSlide = (index) => {
  activeIndex.value = index;
  resumeAutoplay();
};

const prevSlide = () => {
  activeIndex.value = (activeIndex.value - 1 + slides.length) % slides.length;
  resumeAutoplay();
};

const nextSlide = () => {
  activeIndex.value = (activeIndex.value + 1) % slides.length;
};

onMounted(() => {
  resumeAutoplay();
});

onBeforeUnmount(() => {
  clearAutoplay();
});
</script>

<style scoped>
.hero {
  position: relative;
  margin-top: 24px;
  padding: 28px;
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 24px;
  overflow: hidden;
  min-height: 400px;
  background: linear-gradient(135deg, #fff7ef 0%, #fff 44%, #fffaf7 100%);
}

.hero-copy {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: stretch;
  padding: 8px 0;
}

.copy-stack {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 30px;
  border-radius: 20px;
  background: linear-gradient(135deg, #ff8f1f 0%, #ff5000 100%);
  color: #fff;
}

.eyebrow {
  margin: 0 0 14px;
  color: rgba(255, 255, 255, 0.82);
  font-weight: 700;
  letter-spacing: 1.4px;
  font-size: 13px;
}

h1 {
  margin: 0;
  font: 700 clamp(34px, 4.2vw, 54px) / 1.12 var(--font-display);
  white-space: pre-line;
  color: #fff;
  text-wrap: balance;
}

.desc {
  margin: 18px 0 0;
  max-width: 520px;
  color: rgba(255, 255, 255, 0.88);
  font-size: 15px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.hero-btn {
  min-width: 132px;
  min-height: 42px;
  text-align: center;
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.48);
  color: #fff;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.12);
  transition: all 0.22s ease;
}

.btn-outline:hover {
  border-color: rgba(255, 255, 255, 0.74);
  background: rgba(255, 255, 255, 0.18);
}

.category-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.pill {
  padding: 8px 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.86);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  transition: all 0.22s ease;
}

.pill:hover,
.pill.active {
  color: var(--brand);
  background: #fff;
  border-color: #fff;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.12);
}

.hero-visual {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.visual-frame {
  position: relative;
  display: block;
  min-height: 344px;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid #ffd4c1;
  box-shadow: 0 16px 34px rgba(255, 80, 0, 0.12);
}

.visual-frame img {
  width: 100%;
  height: 100%;
  min-height: 344px;
  object-fit: cover;
  display: block;
}

.visual-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(255, 80, 0, 0.18) 0%, rgba(255, 80, 0, 0) 38%),
    linear-gradient(180deg, rgba(60, 60, 60, 0.06) 0%, rgba(60, 60, 60, 0.52) 100%);
}

.visual-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 24px;
  color: #fff;
}

.visual-badge {
  align-self: flex-start;
  padding: 8px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
}

.visual-meta {
  max-width: 72%;
}

.visual-kicker {
  margin: 0 0 8px;
  font-size: 13px;
  letter-spacing: 1.4px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.84);
}

.visual-meta h2 {
  margin: 0;
  font-size: clamp(28px, 3vw, 36px);
  line-height: 1.1;
}

.visual-meta p {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.84);
}

.visual-controls {
  position: absolute;
  right: 20px;
  bottom: 20px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-btn,
.dot {
  border: none;
  cursor: pointer;
}

.nav-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: var(--brand);
  font-size: 28px;
  line-height: 1;
  box-shadow: 0 8px 18px rgba(255, 80, 0, 0.16);
  transition: background 0.22s ease;
}

.nav-btn:hover {
  background: #fff;
}

.dots {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(60, 60, 60, 0.36);
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.48);
  transition: all 0.2s ease;
}

.dot.active {
  width: 28px;
  border-radius: 999px;
  background: #fff;
}

.copy-fade-enter-active,
.copy-fade-leave-active,
.visual-fade-enter-active,
.visual-fade-leave-active {
  transition: opacity 0.34s ease, transform 0.34s ease;
}

.copy-fade-enter-from,
.copy-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.visual-fade-enter-from,
.visual-fade-leave-to {
  opacity: 0;
  transform: scale(0.985);
}

@media (max-width: 1024px) {
  .hero {
    grid-template-columns: 1fr;
    padding: 22px;
  }

  .visual-frame,
  .visual-frame img {
    min-height: 300px;
  }

  .visual-meta {
    max-width: 80%;
  }
}

@media (max-width: 680px) {
  .hero {
    padding: 16px;
    gap: 18px;
    min-height: auto;
  }

  .copy-stack {
    padding: 22px 18px;
  }

  h1 {
    font-size: 34px;
  }

  .desc {
    font-size: 14px;
    line-height: 1.7;
  }

  .hero-actions,
  .category-pills {
    margin-top: 18px;
  }

  .hero-btn {
    min-width: 0;
    flex: 1;
  }

  .visual-frame,
  .visual-frame img {
    min-height: 260px;
  }

  .visual-content {
    padding: 18px;
  }

  .visual-meta {
    max-width: 100%;
  }

  .visual-controls {
    right: 12px;
    bottom: 12px;
    gap: 8px;
  }

  .nav-btn {
    width: 36px;
    height: 36px;
    font-size: 24px;
  }
}
</style>
