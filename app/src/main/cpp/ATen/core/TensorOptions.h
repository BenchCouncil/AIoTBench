#pragma once

#include <ATen/core/Backend.h>
#include <ATen/core/DefaultTensorOptions.h>
#include <ATen/core/Device.h>
#include <ATen/core/Layout.h>
#include <ATen/core/ScalarType.h>
#include <ATen/core/ScalarTypeUtils.h>

#include "c10/util/Optional.h"

#include <cstddef>
#include <iosfwd>
#include <utility>

namespace at {

// Forward declaration from OptionsGuard.h
//
// Hopefully the out-of-line function call is not costing us too much: all this
// function does is return a memory address, so it shouldn't be costing
// us too much optimizer juice.
CAFFE2_API const DefaultTensorOptions& getDefaultTensorOptions();

/// A class to encapsulate construction axes of an Tensor.  TensorOptions was
/// designed to support the Python style API for specifying construction options
/// on factory functions, e.g.,
///
///     torch.zeros(2, 3, dtype=torch.int32)
///
/// Because C++ doesn't natively support keyword arguments, there must be
/// another way of specifying keyword-like arguments.  TensorOptions is a
/// builder class which can be used to construct this "dictionary" of keyword
/// arguments: functions which support TensorOptions conventionally take this
/// argument optionally as their last argument.
///
/// WARNING: In PyTorch, there are `torch::` variants of factory functions,
/// e.g., torch::zeros for at::zeros.  These return Variables (while the
/// stock ATen functions return plain Tensors).  If you mix these functions
/// up, you WILL BE SAD.
///
/// Rather than use the constructor of this class directly, you should prefer to
/// use the constructor functions, and then chain setter methods on top of them.
///
///     at::device(at::kCUDA).dtype(kInt)
///     at::dtype(at::kInt)
///
/// Additionally, anywhere a TensorOptions is expected, you can directly
/// pass at::kCUDA / at::kInt, and it will implicitly convert to a TensorOptions.
///
/// Here are some recommended ways to create a 2x2 tensor of zeros
/// with certain properties.  These all *implicitly* make use of
/// TensorOptions, even if they don't mention the class explicitly:
///
///     at::zeros({2,2}, at::kCUDA);
///     at::zeros({2,2}, at::kLong);
///     at::zeros({2,2}, at::device(at::kCUDA).dtype(at::kLong()));
///     at::zeros({2,2}, at::device({at::kCUDA, 1})); // place on device 1
///     at::zeros({2,2}, at::requires_grad());
///
struct CAFFE2_API TensorOptions {
  TensorOptions()
    : requires_grad_(false)
    , is_variable_(false)
    , has_device_(false)
    , has_dtype_(false)
    , has_layout_(false)
    , has_requires_grad_(false)
    , has_is_variable_(false)
    {}

  /// Constructs a `TensorOptions` object with the given layout.
  /* implicit */ TensorOptions(Layout layout) : TensorOptions() {
    this->set_layout(layout);
  }

  /// Constructs a `TensorOptions` object with the given device.
  /* implicit */ TensorOptions(Device device) : TensorOptions() {
    this->set_device(device);
  }

  /// Constructs a `TensorOptions` object from a backend, forwarded to the
  /// `Device` constructor.
  /* implicit */ TensorOptions(Backend backend)
      : TensorOptions(Device(backendToDeviceType(backend))) {}

  /// Constructs a `TensorOptions` object from a device type, forwarded to the
  /// `Device` constructor.
  /* implicit */ TensorOptions(DeviceType device_type)
      : TensorOptions(Device(device_type)) {}

  /// Constructs a `TensorOptions` object with the given dtype.
  /* implicit */ TensorOptions(ScalarType dtype) : TensorOptions() {
    this->set_dtype(dtype);
  }

  /// True if all elements of the `TensorOptions` match that of the other.
  bool operator==(const TensorOptions& other) const noexcept {
    return
        has_dtype_ == other.has_dtype_ &&
        has_layout_ == other.has_layout_ &&
        has_device_ == other.has_device_ &&
        has_requires_grad_ == other.has_requires_grad_ &&
        has_is_variable_ == other.has_is_variable_ &&
        (!has_dtype_ || dtype_ == other.dtype_) &&
        (!has_layout_ || layout_ == other.layout_) &&
        (!has_device_ || device_ == other.device_) &&
        (!requires_grad_ || requires_grad_ == other.requires_grad_) &&
        (!is_variable_ || is_variable_ == other.is_variable_);
  }

  /// True if any of the elements of this `TensorOptions` do not match that of
  /// the other.
  bool operator!=(const TensorOptions& other) const noexcept {
    return !(*this == other);
  }

  /// Return a copy of `TensorOptions` with `device` set to the given one, or
  /// cleared if `device` is `nullopt`.
  C10_NODISCARD TensorOptions device(optional<Device> device) const noexcept {
    TensorOptions r = *this;
    r.set_device(device);
    return r;
  }

  /// Return a copy of `TensorOptions` with `device` set to the given one.
  /// (This overload ensures that initializer lists for Device work
  /// correctly.)
  C10_NODISCARD TensorOptions device(Device d) const noexcept {
    return device(c10::make_optional(d));
  }

  /// Return a copy of `TensorOptions`, but with device set to CUDA, and the
  /// device index set to the given one.
  ///
  /// TODO: This function encourages bad behavior (assuming CUDA is
  /// the only device that matters).  Get rid of it / rename it.
  C10_NODISCARD TensorOptions device_index(int32_t device_index) const noexcept {
    return device({Device::Type::CUDA, device_index});
  }

  /// Return a copy of `TensorOptions` with `dtype` set to the given one.
  C10_NODISCARD TensorOptions dtype(optional<ScalarType> dtype) const noexcept {
    TensorOptions r = *this;
    r.set_dtype(dtype);
    return r;
  }

  // Since dtype is taken...
  template <typename T>
  TensorOptions& dtype() {
    // TODO: Fix after @roy-li's fix
    dtype_ = CTypeToScalarType<T>::to();
    has_dtype_ = true;
    return *this;
  }

  /// Sets the layout of the `TensorOptions`.
  C10_NODISCARD TensorOptions layout(optional<Layout> layout) const noexcept {
    TensorOptions r = *this;
    r.set_layout(layout);
    return r;
  }

  /// Sets the `requires_grad` property of the `TensorOptions`.
  C10_NODISCARD TensorOptions requires_grad(optional<bool> requires_grad) const noexcept {
    TensorOptions r = *this;
    r.set_requires_grad(requires_grad);
    return r;
  }

  /// Sets the `is_variable` property on the `TensorOptions`.
  C10_NODISCARD TensorOptions is_variable(optional<bool> is_variable) const noexcept {
    TensorOptions r = *this;
    r.set_is_variable(is_variable);
    return r;
  }

  /// Returns the device of the `TensorOptions`.
  Device device() const noexcept {
    return has_device_ ? device_ : getDefaultTensorOptions().device();
  }

  /// Returns the device of the `TensorOptions`, or `c10::nullopt` if
  /// device is not specified.
  optional<Device> device_opt() const noexcept {
    return has_device_ ? c10::make_optional(device_) : c10::nullopt;
  }

  /// Returns the device index of the `TensorOptions`.
  int32_t device_index() const noexcept {
    return device().index();
  }

  /// Returns the dtype of the `TensorOptions`.
  ScalarType dtype() const noexcept {
    return has_dtype_ ? dtype_ : getDefaultTensorOptions().dtype();
  }

  /// Returns the dtype of the `TensorOptions`, or `c10::nullopt` if
  /// device is not specified.
  optional<ScalarType> dtype_opt() const noexcept {
    return has_dtype_ ? c10::make_optional(dtype_) : c10::nullopt;
  }

  /// Returns the layout of the `TensorOptions`.
  Layout layout() const noexcept {
    return has_layout_ ? layout_ : getDefaultTensorOptions().layout();
  }

  /// Returns the layout of the `TensorOptions`, or `c10::nullopt` if
  /// layout is not specified.
  optional<Layout> layout_opt() const noexcept {
    return has_layout_ ? c10::make_optional(layout_) : c10::nullopt;
  }

  /// Returns the `requires_grad` property of the `TensorOptions`.
  bool requires_grad() const noexcept {
    return has_requires_grad_ ? requires_grad_ : getDefaultTensorOptions().requires_grad();
  }

  /// Returns the `requires_grad` property of the `TensorOptions`, or
  /// `c10::nullopt` if `requires_grad` is not specified.
  optional<bool> requires_grad_opt() const noexcept {
    return has_requires_grad_ ? c10::make_optional(requires_grad_)
                              : c10::nullopt;
  }

  /// Returns the `is_variable` property of the `TensorOptions`.
  bool is_variable() const noexcept {
    return has_is_variable_ ? is_variable_ : getDefaultTensorOptions().is_variable();
  }

  /// Returns the `is_variable` property of the `TensorOptions`, or
  /// `c10::nullopt` if `is_variable` is not specified.
  optional<bool> is_variable_opt() const noexcept {
    return has_is_variable_ ? c10::make_optional(is_variable_) : c10::nullopt;
  }

  // Resolves the ATen backend specified by the current construction axes.
  Backend backend() const noexcept {
    Backend backend;
    if (device().type() == Device::Type::CPU) {
      backend = (layout() == kStrided) ? Backend::CPU : Backend::SparseCPU;
    } else {
      backend = (layout() == kStrided) ? Backend::CUDA : Backend::SparseCUDA;
    }
    return backend;
  }

 private:

  // These methods are currently private because I'm not sure if it's wise
  // to actually publish them.  They are methods because I need them in
  // the constructor and the functional API implementation.
  //
  // If you really, really need it, you can make these public, but check if you
  // couldn't just do what you need with the functional API.  Similarly, these
  // methods are not chainable, because if you wanted chaining, you probably
  // want to use the functional API instead.  (It's probably OK to make
  // these chainable, because these functions are all explicitly annotated
  // with a ref-qualifier, the trailing &, that makes them illegal to call
  // on temporaries.)

  /// Mutably set the device of `TensorOptions`.
  void set_device(optional<Device> device) & noexcept {
    if (device) {
      device_ = *device;
      has_device_ = true;
    } else {
      has_device_ = false;
    }
  }

  /// Mutably set the dtype of `TensorOptions`.
  void set_dtype(optional<ScalarType> dtype) & noexcept {
    if (dtype) {
      dtype_ = *dtype;
      has_dtype_ = true;
    } else {
      has_dtype_ = false;
    }
  }

  /// Mutably set the layout of `TensorOptions`.
  void set_layout(optional<Layout> layout) & noexcept {
    if (layout) {
      layout_ = *layout;
      has_layout_ = true;
    } else {
      has_layout_ = false;
    }
  }

  /// Mutably set the `requires_grad` property of `TensorOptions`.
  void set_requires_grad(optional<bool> requires_grad) & noexcept {
    if (requires_grad) {
      requires_grad_ = *requires_grad;
      has_requires_grad_ = true;
    } else {
      has_requires_grad_ = false;
    }
  }

  /// Mutably set the `is_variable` property of `TensorOptions`.
  void set_is_variable(optional<bool> is_variable) & noexcept {
    if (is_variable) {
      is_variable_ = *is_variable;
      has_is_variable_ = true;
    } else {
      has_is_variable_ = false;
    }
  }

  // WARNING: If you edit TensorOptions to add more options, you
  // must adjust the implementation of Tensor::options

  // NB: We didn't use c10::optional here, because then we can't pack
  // the has_***_ boolean fields.

  Device     device_  = at::kCPU; // 64-bit (TODO: this should be 32-bit)

  // Bitmask required here to get this to fit inside 32 bits (or even 64 bits,
  // for that matter)

  ScalarType dtype_   = at::kFloat;  // 8-bit
  Layout     layout_  = at::kStrided; // 8-bit

  bool requires_grad_     : 1;
  bool is_variable_       : 1;

  bool has_device_        : 1;
  bool has_dtype_         : 1;
  bool has_layout_        : 1;
  bool has_requires_grad_ : 1;
  bool has_is_variable_   : 1;
};

// We should aspire to fit in one machine-size word; but a size greater than two
// words is too much.  (We are doing terribly on 32-bit archs, where we require
// three machine size words to store tensor options.  Eek!)
static_assert( sizeof(TensorOptions) <= sizeof(int64_t) * 2,
               "TensorOptions must fit in 128-bits" );

/// Convenience function that returns a `TensorOptions` object with the `dtype`
/// set to the given one.
inline TensorOptions dtype(ScalarType dtype) {
  return TensorOptions().dtype(dtype);
}

/// Convenience function that returns a `TensorOptions` object with the `layout`
/// set to the given one.
inline TensorOptions layout(Layout layout) {
  return TensorOptions().layout(layout);
}

/// Convenience function that returns a `TensorOptions` object with the `device`
/// set to the given one.
inline TensorOptions device(Device device) {
  return TensorOptions().device(std::move(device));
}

/// Convenience function that returns a `TensorOptions` object with the
/// `device` set to CUDA and the `device_index` set to the given one.
inline TensorOptions device_index(int32_t device_index) {
  return TensorOptions().device_index(device_index);
}

/// Convenience function that returns a `TensorOptions` object with the
/// `requires_grad` set to the given one.
inline TensorOptions requires_grad(bool requires_grad = true) {
  return TensorOptions().requires_grad(requires_grad);
}

std::ostream& operator<<(
    std::ostream& stream,
    const TensorOptions& options);


DefaultTensorOptions& DefaultTensorOptions::merge(const TensorOptions& options) {
  if (options.dtype_opt().has_value()) {
    dtype_ = options.dtype();
  }
  if (options.device_opt().has_value()) {
    device_ = options.device();
  }
  if (options.layout_opt().has_value()) {
    layout_ = options.layout();
  }
  if (options.requires_grad_opt().has_value()) {
    requires_grad_ = options.requires_grad();
  }
  if (options.is_variable_opt().has_value()) {
    is_variable_ = options.is_variable();
  }
  return *this;
}

template <typename T>
inline TensorOptions dtype() {
  // TODO: Fix after @roy-li's fix
  return dtype(CTypeToScalarType<T>::to());
}

} // namespace at
