import { Link } from "react-router-dom";

export default function Unauthorized() {
  return (
    <div className="flex-1 flex items-center justify-center min-h-[70vh]">
      <div className="w-full max-w-[440px] bg-surface rounded-xl border border-outline-variant p-8 shadow-sm text-center">
        <div className="mx-auto mb-6 w-16 h-16 rounded-full bg-danger-bg flex items-center justify-center">
          <span className="material-symbols-outlined text-danger text-[32px]">block</span>
        </div>
        <h1 className="font-h1 text-on-surface mb-2">Bu sayfaya erişim yetkiniz yok</h1>
        <p className="font-body-md text-on-surface-variant mb-8">
          Hesabınızın rolü bu sayfayı görüntülemek için yeterli yetkiye sahip değil. Erişime
          ihtiyacınız olduğunu düşünüyorsanız yöneticinizle iletişime geçin.
        </p>
        <Link
          to="/overview"
          className="inline-flex items-center justify-center gap-2 h-11 px-5 bg-primary hover:bg-[#0033b3] text-on-primary font-label-md rounded transition-colors"
        >
          <span className="material-symbols-outlined text-[18px]">arrow_back</span>
          Genel Bakış'a Dön
        </Link>
      </div>
    </div>
  );
}
